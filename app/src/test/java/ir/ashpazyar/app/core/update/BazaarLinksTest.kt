package ir.ashpazyar.app.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BazaarLinksTest {
    private val app = "ir.chashika.app"

    @Test
    fun acceptsOnlyOfficialBazaarListingAndDeepLink() {
        assertTrue(BazaarLinks.isAllowedStoreUrl("https://cafebazaar.ir/app/ir.chashika.app", app))
        assertTrue(BazaarLinks.isAllowedStoreUrl("https://www.cafebazaar.ir/app/ir.chashika.app/", app))
        assertTrue(BazaarLinks.isAllowedStoreUrl("bazaar://details?id=ir.chashika.app", app))
    }

    @Test
    fun rejectsDirectApkAndLookalikeHosts() {
        assertFalse(BazaarLinks.isAllowedStoreUrl("https://cafebazaar.ir/app/other.app", app))
        assertFalse(BazaarLinks.isAllowedStoreUrl("https://cafebazaar.ir.evil.example/app/ir.chashika.app", app))
        assertFalse(BazaarLinks.isAllowedStoreUrl("https://dl.cafebazaar.ir/app/ir.chashika.app.apk", app))
        assertFalse(BazaarLinks.isAllowedStoreUrl("https://github.com/example/app.apk", app))
        assertFalse(BazaarLinks.isAllowedStoreUrl("http://cafebazaar.ir/app/ir.chashika.app", app))
        assertFalse(BazaarLinks.isAllowedStoreUrl("bazaar://details?id=other.app", app))
    }
}
