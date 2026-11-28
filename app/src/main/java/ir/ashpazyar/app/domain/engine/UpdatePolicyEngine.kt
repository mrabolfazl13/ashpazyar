package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.core.update.SemVer
import ir.ashpazyar.app.domain.model.UpdateDecision
import ir.ashpazyar.app.domain.model.UpdateManifest
import ir.ashpazyar.app.domain.model.UpdatePolicy

/**
 * Pure update decision logic. It deliberately has no Android or network
 * dependency so every policy branch can be unit-tested.
 *
 * A new release is never mandatory merely because it exists: the manifest must
 * explicitly carry force_update=true and an applicable forced/minimum policy.
 */
object UpdatePolicyEngine {

    fun evaluate(
        manifest: UpdateManifest,
        currentVersion: String,
        currentVersionCode: Long
    ): UpdateDecision {
        val latest = SemVer.parse(manifest.latestVersion)
            ?: return UpdateDecision.Invalid("نسخه جدید در مانیفست معتبر نیست")
        val current = SemVer.parse(currentVersion)
            ?: return UpdateDecision.Invalid("نسخه فعلی برنامه قابل مقایسه نیست")
        val policy = UpdatePolicy.from(manifest.updatePolicy)
            ?: return UpdateDecision.Invalid("سیاست به‌روزرسانی ناشناخته است")
        val minimum = manifest.minimumSupportedVersion?.let { SemVer.parse(it) }
        if (manifest.minimumSupportedVersion != null && minimum == null) {
            return UpdateDecision.Invalid("حداقل نسخه پشتیبانی‌شده معتبر نیست")
        }
        if (minimum != null && minimum > latest) {
            return UpdateDecision.Invalid("حداقل نسخه از نسخه جدید بزرگ‌تر است")
        }
        if (policy == UpdatePolicy.MINIMUM_VERSION && minimum == null) {
            return UpdateDecision.Invalid("سیاست حداقل نسخه به حداقل نسخه نیاز دارد")
        }
        if (manifest.platform == "android") {
            val latestCode = manifest.versionCode
                ?: return UpdateDecision.Invalid("version_code برای اندروید الزامی است")
            if (latestCode <= 0) return UpdateDecision.Invalid("version_code نامعتبر است")
        }

        if (latest <= current) return UpdateDecision.UpToDate

        val latestCode = manifest.versionCode
        if (latestCode != null && latestCode <= currentVersionCode) {
            return UpdateDecision.Invalid("نسخه جدید کد بیلد بالاتری ندارد؛ به‌روزرسانی رد شد")
        }

        val mandatory = manifest.forceUpdate && when (policy) {
            UpdatePolicy.FORCED -> true
            UpdatePolicy.MINIMUM_VERSION -> minimum != null && current < minimum
            UpdatePolicy.OPTIONAL -> false
        }
        return UpdateDecision.Available(manifest, mandatory)
    }
}
