package ir.ashpazyar.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {
    @Test
    fun parsesStrictSemanticVersions() {
        val version = SemVer.parse("1.2.3-alpha.1+build.9")
        assertEquals(1, version?.major)
        assertEquals(2, version?.minor)
        assertEquals(3, version?.patch)
        assertEquals(listOf("alpha", "1"), version?.preRelease)
        assertEquals(listOf("build", "9"), version?.buildMetadata)
    }

    @Test
    fun ordersMajorMinorAndPatchNumerically() {
        assertTrue(SemVer.parse("2.0.0")!! > SemVer.parse("1.99.99")!!)
        assertTrue(SemVer.parse("1.10.0")!! > SemVer.parse("1.9.0")!!)
        assertTrue(SemVer.parse("1.2.10")!! > SemVer.parse("1.2.9")!!)
    }

    @Test
    fun prereleaseSortsBeforeRelease() {
        assertTrue(SemVer.parse("1.0.0-rc.1")!! < SemVer.parse("1.0.0")!!)
        assertTrue(SemVer.parse("1.0.0-alpha")!! < SemVer.parse("1.0.0-beta")!!)
        assertTrue(SemVer.parse("1.0.0-alpha")!! < SemVer.parse("1.0.0-alpha.1")!!)
        assertTrue(SemVer.parse("1.0.0-alpha.2")!! < SemVer.parse("1.0.0-alpha.10")!!)
    }

    @Test
    fun buildMetadataDoesNotAffectOrdering() {
        assertEquals(0, SemVer.parse("1.2.3+one")!!.compareTo(SemVer.parse("1.2.3+two")!!))
    }

    @Test
    fun rejectsInvalidVersions() {
        assertNull(SemVer.parse("1.2"))
        assertNull(SemVer.parse("1.2.3.4"))
        assertNull(SemVer.parse("01.2.3"))
        assertNull(SemVer.parse("1.02.3"))
        assertNull(SemVer.parse("1.2.03"))
        assertNull(SemVer.parse("1.2.3-01"))
        assertNull(SemVer.parse("1.2.3-"))
        assertNull(SemVer.parse("1.2.3+"))
        assertNull(SemVer.parse("1.2.3-rc..1"))
    }
}
