package ir.ashpazyar.app.domain.engine

import ir.ashpazyar.app.domain.model.UpdateDecision
import ir.ashpazyar.app.domain.model.UpdateManifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyEngineTest {
    private fun manifest(
        latest: String = "1.3.0",
        minimum: String? = "1.2.0",
        policy: String = "optional",
        force: Boolean = false,
        versionCode: Long = 6
    ) = UpdateManifest(
        productId = "ashpazyar",
        platform = "android",
        latestVersion = latest,
        minimumSupportedVersion = minimum,
        forceUpdate = force,
        updatePolicy = policy,
        releaseChannel = "stable",
        downloadUrl = "https://cafebazaar.ir/app/ir.ashpazyar.app",
        sha256 = "a".repeat(64),
        publishedAt = "2026-11-28T10:00:00Z",
        versionCode = versionCode
    )

    private fun available(decision: UpdateDecision): UpdateDecision.Available {
        assertTrue(decision is UpdateDecision.Available)
        return decision as UpdateDecision.Available
    }

    @Test
    fun newOptionalReleaseIsNotMandatory() {
        val decision = available(UpdatePolicyEngine.evaluate(manifest(), "1.2.2", 5))
        assertFalse(decision.mandatory)
    }

    @Test
    fun forcedPolicyBecomesMandatoryOnlyWithForceFlag() {
        val forced = available(UpdatePolicyEngine.evaluate(manifest(policy = "forced", force = true), "1.2.2", 5))
        assertTrue(forced.mandatory)

        val disabled = available(UpdatePolicyEngine.evaluate(manifest(policy = "forced", force = false), "1.2.2", 5))
        assertFalse(disabled.mandatory)
    }

    @Test
    fun minimumPolicyForcesOnlyOlderVersions() {
        val older = available(UpdatePolicyEngine.evaluate(manifest(policy = "minimum_version", force = true), "1.1.9", 4))
        assertTrue(older.mandatory)

        val equal = available(UpdatePolicyEngine.evaluate(manifest(policy = "minimum_version", force = true), "1.2.0", 5))
        assertFalse(equal.mandatory)
    }

    @Test
    fun optionalPolicyNeverForcesEvenIfFlagIsSet() {
        val decision = available(UpdatePolicyEngine.evaluate(manifest(policy = "optional", force = true), "1.0.0", 1))
        assertFalse(decision.mandatory)
    }

    @Test
    fun equalOrLowerVersionIsUpToDate() {
        assertTrue(UpdatePolicyEngine.evaluate(manifest(latest = "1.2.2"), "1.2.2", 5) is UpdateDecision.UpToDate)
        assertTrue(UpdatePolicyEngine.evaluate(manifest(latest = "1.2.1"), "1.2.2", 5) is UpdateDecision.UpToDate)
    }

    @Test
    fun versionCodeDowngradeIsRejected() {
        assertTrue(UpdatePolicyEngine.evaluate(manifest(versionCode = 5), "1.2.2", 5) is UpdateDecision.Invalid)
    }

    @Test
    fun invalidMinimumIsRejected() {
        assertTrue(UpdatePolicyEngine.evaluate(manifest(minimum = "2.0.0"), "1.2.2", 5) is UpdateDecision.Invalid)
    }
}
