package ir.ashpazyar.app.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import ir.ashpazyar.app.core.update.BazaarLinks

/** Opens Cafe Bazaar only. This app never downloads or installs an APK directly. */
object BazaarLauncher {
    fun open(context: Context, appPackage: String): Boolean {
        val bazaarIntent = Intent(Intent.ACTION_VIEW, Uri.parse(BazaarLinks.deepLink(appPackage)))
            .setPackage(BazaarLinks.PACKAGE_NAME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, bazaarIntent)) return true

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(BazaarLinks.listingUrl(appPackage)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return tryStart(context, webIntent)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
