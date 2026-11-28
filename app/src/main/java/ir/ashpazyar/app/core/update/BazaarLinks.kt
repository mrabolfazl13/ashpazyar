package ir.ashpazyar.app.core.update

import java.net.URI

/** Android builds may only send users to Cafe Bazaar, never to a direct APK URL. */
object BazaarLinks {
    const val PACKAGE_NAME = "com.farsitel.bazaar"
    const val STORE_HOST = "cafebazaar.ir"

    fun listingUrl(appPackage: String): String = "https://$STORE_HOST/app/$appPackage"

    fun deepLink(appPackage: String): String = "bazaar://details?id=$appPackage"

    fun isAllowedStoreUrl(raw: String, appPackage: String): Boolean {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (uri.scheme.equals("https", ignoreCase = true)) {
            val host = uri.host?.lowercase() ?: return false
            if (host != STORE_HOST && host != "www.$STORE_HOST") return false
            val expectedPath = "/app/$appPackage"
            return uri.path?.trimEnd('/') == expectedPath
        }
        if (uri.scheme.equals("bazaar", ignoreCase = true)) {
            if (!uri.host.equals("details", ignoreCase = true)) return false
            val query = uri.rawQuery ?: return false
            return query.split('&').any { part ->
                val pieces = part.split('=', limit = 2)
                pieces.size == 2 && pieces[0] == "id" && pieces[1] == appPackage
            }
        }
        return false
    }
}
