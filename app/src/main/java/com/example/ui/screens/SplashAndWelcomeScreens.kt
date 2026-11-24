package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppSettings
import com.example.ui.components.NabdLogoIcon
import com.example.ui.components.TelegramFollowSection
import com.example.ui.theme.CyanVioletBrush
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isFirstLaunch: Boolean,
    onSplashFinished: (goToWelcome: Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val infinite = rememberInfiniteTransition(label = "splash_pulse")
    val ringScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_scale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_alpha"
    )

    LaunchedEffect(isFirstLaunch) {
        delay(1300L)
        onSplashFinished(isFirstLaunch)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF151C3D), NabdBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NabdCyan, NabdViolet, NabdPink))
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.nabd_tv_icon),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.appTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = NabdCyan,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WelcomeLanguageScreen(
    settings: AppSettings,
    onLanguagePicked: (String) -> Unit,
    onContinue: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedLang by remember(settings.language) { mutableStateOf(settings.language) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            NabdLogoIcon(size = 84.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                text = strings.chooseLanguageTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.chooseLanguageSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = NabdTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LanguageOptionCard(
                    label = strings.arabicLabel,
                    selected = selectedLang == "ar",
                    onClick = {
                        selectedLang = "ar"
                        onLanguagePicked("ar")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("welcome_lang_ar")
                )
                LanguageOptionCard(
                    label = strings.englishLabel,
                    selected = selectedLang == "en",
                    onClick = {
                        selectedLang = "en"
                        onLanguagePicked("en")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("welcome_lang_en")
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = { onContinue(selectedLang) },
                colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_continue_button")
            ) {
                Text(
                    text = strings.getStarted,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(28.dp))

            TelegramFollowSection(
                botUrl = settings.telegramBotUrl,
                contactUrl = settings.telegramContactUrl,
                channelUrl = settings.telegramChannelUrl
            )
        }
    }
}

@Composable
private fun LanguageOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) NabdViolet.copy(alpha = 0.22f) else NabdSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            brush = if (selected) CyanVioletBrush else Brush.linearGradient(listOf(NabdBorder, NabdBorder))
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Language,
                contentDescription = label,
                tint = if (selected) NabdCyan else NabdTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
    }
}
