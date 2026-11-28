package ir.ashpazyar.app.core.update

import ir.ashpazyar.app.domain.model.UpdateManifest
import ir.ashpazyar.app.domain.model.UpdatePolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.PublicKey
import java.security.Signature
import java.time.Instant
import java.util.Base64

class UpdateManifestException(message: String) : Exception(message)

/**
 * Verifies the RSA/SHA-256 signature over the canonical payload before any
 * field is trusted as an update instruction.
 */
class UpdateManifestVerifier(
    private val publicKey: PublicKey,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun verify(
        raw: String,
        expectedProduct: String,
        expectedPlatform: String,
        expectedChannel: String,
        appPackage: String
    ): UpdateManifest {
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_MANIFEST_BYTES) {
            throw UpdateManifestException("حجم مانیفست بیش از حد مجاز است")
        }
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }
            .getOrElse { throw UpdateManifestException("مانیفست JSON معتبر نیست") }
        if (root.keys != setOf("payload", "manifest_signature")) {
            throw UpdateManifestException("ساختار مانیفست ناشناخته است")
        }
        val payload = root["payload"] as? JsonObject
            ?: throw UpdateManifestException("payload مانیفست وجود ندارد")
        val signatureText = (root["manifest_signature"] as? JsonPrimitive)?.content
            ?: throw UpdateManifestException("امضای مانیفست وجود ندارد")
        val signature = runCatching { Base64.getDecoder().decode(signatureText) }
            .getOrElse { throw UpdateManifestException("امضای مانیفست Base64 معتبر نیست") }

        val signedBytes = CanonicalJson.encode(payload).toByteArray(Charsets.UTF_8)
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update(signedBytes)
        if (!verifier.verify(signature)) {
            throw UpdateManifestException("امضای مانیفست معتبر نیست")
        }

        val manifest = runCatching { json.decodeFromJsonElement<UpdateManifest>(payload) }
            .getOrElse { throw UpdateManifestException("فیلدهای مانیفست کامل یا معتبر نیستند") }
        validate(manifest, expectedProduct, expectedPlatform, expectedChannel, appPackage)
        return manifest
    }

    private fun validate(
        manifest: UpdateManifest,
        expectedProduct: String,
        expectedPlatform: String,
        expectedChannel: String,
        appPackage: String
    ) {
        if (manifest.productId != expectedProduct) {
            throw UpdateManifestException("محصول مانیفست با این برنامه هم‌خوان نیست")
        }
        if (manifest.platform != expectedPlatform) {
            throw UpdateManifestException("پلتفرم مانیفست با این برنامه هم‌خوان نیست")
        }
        if (manifest.releaseChannel != expectedChannel) {
            throw UpdateManifestException("کانال انتشار مانیفست با این برنامه هم‌خوان نیست")
        }
        if (SemVer.parse(manifest.latestVersion) == null) {
            throw UpdateManifestException("شماره نسخه جدید معتبر نیست")
        }
        val minimum = manifest.minimumSupportedVersion?.let(SemVer::parse)
        if (manifest.minimumSupportedVersion != null && minimum == null) {
            throw UpdateManifestException("حداقل نسخه پشتیبانی‌شده معتبر نیست")
        }
        val latest = SemVer.parse(manifest.latestVersion)!!
        if (minimum != null && minimum > latest) {
            throw UpdateManifestException("حداقل نسخه از نسخه جدید بزرگ‌تر است")
        }
        val policy = UpdatePolicy.from(manifest.updatePolicy)
            ?: throw UpdateManifestException("سیاست به‌روزرسانی ناشناخته است")
        if (policy == UpdatePolicy.MINIMUM_VERSION && minimum == null) {
            throw UpdateManifestException("سیاست حداقل نسخه به حداقل نسخه نیاز دارد")
        }
        if (!manifest.sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            throw UpdateManifestException("هش SHA-256 مانیفست معتبر نیست")
        }
        if (runCatching { Instant.parse(manifest.publishedAt) }.isFailure) {
            throw UpdateManifestException("زمان انتشار مانیفست معتبر نیست")
        }
        if (manifest.releaseNotes.length > 10_000) {
            throw UpdateManifestException("توضیحات نسخه بیش از حد طولانی است")
        }
        if (manifest.platform == "android") {
            if (manifest.versionCode == null || manifest.versionCode <= 0) {
                throw UpdateManifestException("version_code برای اندروید الزامی است")
            }
            if (!BazaarLinks.isAllowedStoreUrl(manifest.downloadUrl, appPackage)) {
                throw UpdateManifestException("به‌روزرسانی اندروید فقط باید از بازار انجام شود")
            }
        } else if (!manifest.downloadUrl.startsWith("https://")) {
            throw UpdateManifestException("لینک دانلود باید HTTPS باشد")
        }
    }

    companion object {
        private const val MAX_MANIFEST_BYTES = 64 * 1024
    }
}
