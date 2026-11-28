package ir.ashpazyar.app.core.update

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class UpdateManifestVerifierTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val verifier = UpdateManifestVerifier(keyPair.public)

    private fun payload(overrides: Map<String, Any?> = emptyMap()): JsonObject {
        val builder = buildJsonObject {
            put("product_id", "ashpazyar")
            put("platform", "android")
            put("latest_version", "1.3.0")
            put("minimum_supported_version", "1.2.0")
            put("force_update", false)
            put("update_policy", "optional")
            put("release_channel", "stable")
            put("download_url", "https://cafebazaar.ir/app/ir.ashpazyar.app")
            put("sha256", "a".repeat(64))
            put("release_notes", "بهبود تجربه آشپزخانه")
            put("published_at", "2026-11-28T10:00:00Z")
            put("version_code", 6)
        }
        val fields = builder.toMutableMap()
        overrides.forEach { (key, value) ->
            if (value == null) fields.remove(key) else fields[key] = JsonPrimitive(value.toString())
        }
        return JsonObject(fields)
    }

    private fun signed(payload: kotlinx.serialization.json.JsonObject): String {
        val data = CanonicalJson.encode(payload).toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }
        val root = buildJsonObject {
            put("payload", payload)
            put("manifest_signature", JsonPrimitive(Base64.getEncoder().encodeToString(signature)))
        }
        return root.toString()
    }

    @Test
    fun acceptsValidlySignedManifest() {
        val manifest = verifier.verify(
            raw = signed(payload()),
            expectedProduct = "ashpazyar",
            expectedPlatform = "android",
            expectedChannel = "stable",
            appPackage = "ir.ashpazyar.app"
        )
        assertEquals("1.3.0", manifest.latestVersion)
        assertEquals(6L, manifest.versionCode)
    }

    @Test
    fun rejectsTamperedPayload() {
        val signedManifest = signed(payload())
        val tampered = signedManifest.replace("بهبود تجربه آشپزخانه", "متن دست‌کاری‌شده")
        assertThrows(UpdateManifestException::class.java) {
            verifier.verify(tampered, "ashpazyar", "android", "stable", "ir.ashpazyar.app")
        }
    }

    @Test
    fun rejectsWrongProductOrChannel() {
        val raw = signed(payload())
        assertThrows(UpdateManifestException::class.java) {
            verifier.verify(raw, "sanadyar", "android", "stable", "ir.ashpazyar.app")
        }
        assertThrows(UpdateManifestException::class.java) {
            verifier.verify(raw, "ashpazyar", "android", "beta", "ir.ashpazyar.app")
        }
    }

    @Test
    fun rejectsNonBazaarAndroidDownload() {
        val raw = signed(payload(mapOf("download_url" to "https://github.com/example/app.apk")))
        assertThrows(UpdateManifestException::class.java) {
            verifier.verify(raw, "ashpazyar", "android", "stable", "ir.ashpazyar.app")
        }
    }

    @Test
    fun rejectsMissingVersionCodeForAndroid() {
        val raw = signed(payload(mapOf("version_code" to null)))
        assertThrows(UpdateManifestException::class.java) {
            verifier.verify(raw, "ashpazyar", "android", "stable", "ir.ashpazyar.app")
        }
    }

    @Test
    fun acceptsManifestSignedByPublisherTool() {
        val resource = requireNotNull(javaClass.getResourceAsStream("/update/stable-1.3.0.json"))
        val raw = resource.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val publicPem = requireNotNull(
            javaClass.getResourceAsStream("/update/update_public_key.pem")
        ).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val body = publicPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(body))
        )
        val manifest = UpdateManifestVerifier(publicKey).verify(
            raw = raw,
            expectedProduct = "ashpazyar",
            expectedPlatform = "android",
            expectedChannel = "stable",
            appPackage = "ir.ashpazyar.app"
        )
        assertEquals("1.3.0", manifest.latestVersion)
    }
}
