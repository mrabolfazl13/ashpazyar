package ir.ashpazyar.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    @SerialName("product_id") val productId: String,
    val platform: String,
    @SerialName("latest_version") val latestVersion: String,
    @SerialName("minimum_supported_version") val minimumSupportedVersion: String? = null,
    @SerialName("force_update") val forceUpdate: Boolean = false,
    @SerialName("update_policy") val updatePolicy: String = "optional",
    @SerialName("release_channel") val releaseChannel: String = "stable",
    @SerialName("download_url") val downloadUrl: String,
    val sha256: String,
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("published_at") val publishedAt: String,
    @SerialName("version_code") val versionCode: Long? = null,
    @SerialName("apk_size") val apkSize: Long? = null
)

enum class UpdatePolicy(val wireValue: String) {
    OPTIONAL("optional"),
    MINIMUM_VERSION("minimum_version"),
    FORCED("forced");

    companion object {
        fun from(value: String): UpdatePolicy? = entries.firstOrNull { it.wireValue == value }
    }
}

sealed interface UpdateDecision {
    data object UpToDate : UpdateDecision
    data class Available(val manifest: UpdateManifest, val mandatory: Boolean) : UpdateDecision
    data class Invalid(val reason: String) : UpdateDecision
}
