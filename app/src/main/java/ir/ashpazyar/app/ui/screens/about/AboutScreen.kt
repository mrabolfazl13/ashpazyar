package ir.ashpazyar.app.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.ashpazyar.app.ui.components.*
import ir.ashpazyar.app.ui.theme.AshpaTheme

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = AshpaTheme.palette
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        AshpaTopBar(title = "درباره آشپزیار", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(palette.forest),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Restaurant, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("آشپزیار", style = MaterialTheme.typography.displaySmall, color = palette.ink)
            Text("همراه روزمره آشپزخانه ایرانی", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(5.dp))
            Text("نسخه ۱.۰.۰", style = MaterialTheme.typography.labelMedium, color = palette.terracotta)
            Spacer(Modifier.height(18.dp))
            AshpaCard(Modifier.fillMaxWidth(), corner = 22.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "آشپزیار برای این ساخته شده که تصمیم‌های روزمره آشپزی ساده‌تر شوند: چه بپزیم، چه داریم، چه بخریم و چطور کمتر دور بریزیم.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            ValueRow(Icons.Rounded.WifiOff, "کاملاً آفلاین", "هیچ داده‌ای از دستگاه شما خارج نمی‌شود و برنامه به اینترنت نیاز ندارد.")
            ValueRow(Icons.Rounded.PersonOff, "بدون حساب کاربری", "برای شروع، ثبت‌نام یا شماره تلفن لازم نیست.")
            ValueRow(Icons.Rounded.Block, "بدون تبلیغ و ردیابی", "هیچ تبلیغ، ابزار تحلیلی یا فروشگاه درون‌برنامه‌ای وجود ندارد.")
            ValueRow(Icons.Rounded.Lock, "داده‌ها روی دستگاه شما", "انبار، برنامه غذایی و خرید فقط در حافظه همین دستگاه ذخیره می‌شوند.")
            Spacer(Modifier.height(4.dp))
            AshpaCard(Modifier.fillMaxWidth(), corner = 20.dp) {
                Column(Modifier.padding(15.dp)) {
                    Text("ساخته‌شده با", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Kotlin · Jetpack Compose · Room · Hilt · WorkManager · Coroutines",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "طراحی و توسعه مستقل، با احترام به آشپزی خانگی ایرانی.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AshpaCard(Modifier.fillMaxWidth(), corner = 20.dp) {
                Column(Modifier.padding(15.dp)) {
                    Text("مجوزها", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "کتابخانه‌های متن‌باز استفاده‌شده تحت مجوز Apache 2.0 هستند. محتوای دستورها و نکته‌ها برای این برنامه تهیه شده است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            InlineNotice(
                "آشپزیار جایگزین توصیه‌های پزشکی یا تغذیه تخصصی نیست. برای رژیم‌های درمانی با متخصص مشورت کنید.",
                tone = "warm"
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ValueRow(icon: ImageVector, title: String, description: String) {
    val palette = AshpaTheme.palette
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(palette.sageSoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = palette.forest, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            Text(description, style = MaterialTheme.typography.labelSmall, color = palette.muted)
        }
    }
}
