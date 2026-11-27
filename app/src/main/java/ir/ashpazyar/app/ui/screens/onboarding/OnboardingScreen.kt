package ir.ashpazyar.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ashpazyar.app.ui.components.PrimaryButton
import ir.ashpazyar.app.ui.components.SecondaryButton
import ir.ashpazyar.app.ui.theme.AshpaTheme
import kotlinx.coroutines.launch

private data class OnboardPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val palette = AshpaTheme.palette
    val pages = listOf(
        OnboardPage(
            "آشپزخانه‌تان را بشناسید",
            "مواد موجود، تاریخ انقضا و مقدارشان را ثبت کنید. آشپزیار می‌گوید با همین‌ها چه می‌شود پخت.",
            Icons.Rounded.Kitchen,
            palette.terracotta
        ),
        OnboardPage(
            "هفته را بی‌دغدغه برنامه‌ریزی کنید",
            "وعده‌های هفته را بچینید، فهرست خرید هوشمند بگیرید و هزینه‌ها را از قبل ببینید.",
            Icons.Rounded.CalendarMonth,
            palette.sage
        ),
        OnboardPage(
            "با اطمینان بپزید",
            "حالت پخت گام‌به‌گام، تایمر مرحله‌ها، تنظیم مقدار و نکته‌های ایمنی همراه شماست.",
            Icons.Rounded.SoupKitchen,
            palette.peach
        ),
        OnboardPage(
            "کمتر دور بریزید",
            "باقی‌مانده‌ها را ثبت کنید و پیش از خراب شدن، یادآوری بگیرید. آشپزی خوب، آشپزی مسئولانه است.",
            Icons.Rounded.Eco,
            palette.sage
        )
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(palette.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("آشپزیار", style = MaterialTheme.typography.titleMedium, color = palette.forest)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                viewModel.complete()
                onFinished()
            }) { Text("رد کردن", color = palette.muted) }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(46.dp)).background(palette.forest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(page.icon, null, tint = Color(0xFFE6B88C), modifier = Modifier.size(78.dp))
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(38.dp).clip(CircleShape).background(page.accent),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.AutoAwesome, null, tint = palette.forest, modifier = Modifier.size(19.dp)) }
                }
                Spacer(Modifier.height(30.dp))
                Text(page.title, style = MaterialTheme.typography.displaySmall, color = palette.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(11.dp))
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp).height(7.dp)
                        .width(if (index == pagerState.currentPage) 24.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) palette.terracotta else palette.line)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            if (pagerState.currentPage == pages.lastIndex) {
                PrimaryButton(
                    "شروع آشپزی",
                    onClick = {
                        viewModel.complete()
                        onFinished()
                    },
                    icon = Icons.Rounded.Restaurant
                )
            } else {
                PrimaryButton(
                    "بعدی",
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    icon = Icons.Rounded.ArrowBack
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    "رد کردن",
                    onClick = {
                        viewModel.complete()
                        onFinished()
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
