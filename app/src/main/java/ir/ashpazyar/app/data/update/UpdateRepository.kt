package ir.ashpazyar.app.data.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ashpazyar.app.BuildConfig
import ir.ashpazyar.app.R
import ir.ashpazyar.app.core.update.BazaarLinks
import ir.ashpazyar.app.core.update.UpdateManifestVerifier
import ir.ashpazyar.app.domain.engine.UpdatePolicyEngine
import ir.ashpazyar.app.domain.model.UpdateDecision
import ir.ashpazyar.app.domain.model.UpdateManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateCheckResult {
    data object Disabled : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Dismissed : UpdateCheckResult
    data class Available(val manifest: UpdateManifest, val mandatory: Boolean) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: UpdateHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val preferences: SharedPreferences =
        context.getSharedPreferences("update_preferences", Context.MODE_PRIVATE)

    private val publicKey: PublicKey by lazy { loadPublicKey(context) }

    suspend fun check(manual: Boolean): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!BuildConfig.UPDATE_ENABLED || BuildConfig.DEBUG) return@withContext UpdateCheckResult.Disabled
        try {
            val raw = client.fetch(manifestUrl(), MAX_MANIFEST_BYTES)
            val manifest = UpdateManifestVerifier(publicKey, json).verify(
                raw = raw,
                expectedProduct = BuildConfig.UPDATE_PRODUCT_ID,
                expectedPlatform = BuildConfig.UPDATE_PLATFORM,
                expectedChannel = BuildConfig.UPDATE_CHANNEL,
                appPackage = context.packageName
            )
            if (!BazaarLinks.isAllowedStoreUrl(manifest.downloadUrl, context.packageName)) {
                return@withContext UpdateCheckResult.Failed("به‌روزرسانی فقط از بازار مجاز است")
            }
            when (val decision = UpdatePolicyEngine.evaluate(
                manifest = manifest,
                currentVersion = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE.toLong()
            )) {
                UpdateDecision.UpToDate -> UpdateCheckResult.UpToDate
                is UpdateDecision.Invalid -> UpdateCheckResult.Failed(decision.reason)
                is UpdateDecision.Available -> {
                    if (!decision.mandatory && !manual && dismissedVersion() == manifest.latestVersion) {
                        UpdateCheckResult.Dismissed
                    } else {
                        UpdateCheckResult.Available(manifest, decision.mandatory)
                    }
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            UpdateCheckResult.Failed("بررسی به‌روزرسانی ممکن نشد. اتصال اینترنت را بررسی کنید.")
        }
    }

    fun dismiss(version: String) {
        preferences.edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }

    private fun dismissedVersion(): String? = preferences.getString(KEY_DISMISSED_VERSION, null)

    private fun manifestUrl(): String {
        val base = BuildConfig.UPDATE_MANIFEST_BASE_URL.trimEnd('/')
        return "$base/${BuildConfig.UPDATE_PRODUCT_ID}/${BuildConfig.UPDATE_PLATFORM}/${BuildConfig.UPDATE_CHANNEL}.json"
    }

    private fun loadPublicKey(context: Context): PublicKey {
        val pem = context.resources.openRawResource(R.raw.update_public_key)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val body = pem
            .replace(Regex("-----BEGIN PUBLIC KEY-----"), "")
            .replace(Regex("-----END PUBLIC KEY-----"), "")
            .replace(Regex("\\s"), "")
        val bytes = Base64.decode(body, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes))
    }

    companion object {
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val MAX_MANIFEST_BYTES = 64 * 1024
    }
}

@Singleton
class UpdateHttpClient @Inject constructor() {
    suspend fun fetch(url: String, maxBytes: Int): String = withContext(Dispatchers.IO) {
        val parsed = URL(url)
        if (!parsed.protocol.equals("https", ignoreCase = true)) {
            throw IOException("Only HTTPS update manifests are allowed")
        }
        val connection = parsed.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("User-Agent", "Ghashogh-Android-Update/1")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Manifest HTTP $code")
            val declared = connection.contentLengthLong
            if (declared > maxBytes) throw IOException("Manifest too large")
            val stream = connection.inputStream
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            stream.use {
                while (true) {
                    val read = it.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw IOException("Manifest too large")
                    output.write(buffer, 0, read)
                }
            }
            String(output.toByteArray(), Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }
}
