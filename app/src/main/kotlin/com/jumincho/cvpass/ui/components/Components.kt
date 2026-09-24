package com.jumincho.cvpass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jumincho.cvpass.R
import com.jumincho.cvpass.nfc.NfcAvailability
import com.jumincho.cvpass.nfc.openNfcSettings
import com.jumincho.cvpass.ui.theme.BrandColors
import com.jumincho.cvpass.ui.theme.CvPassTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The CV-PASS mark from the launcher icon, drawn so that it scales cleanly. */
@Composable
fun CvPassLogo(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(BrandColors.Navy)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        val name = stringResource(R.string.app_name)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = BrandColors.Blue)) { append(name.dropLast(1)) }
                withStyle(SpanStyle(color = Color.White)) { append(name.takeLast(1)) }
            },
            fontSize = with(LocalDensity.current) { (size * 0.19f).toSp() },
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** A screen with a top app bar; [onBack] adds an up button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

/** A section title that screen readers announce as a heading. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

/** How a [MessageCard] should read: neutral, good news, waiting, or a problem. */
enum class MessageTone { INFO, SUCCESS, PENDING, ERROR }

/** An inline message with an icon, an optional title and an optional action. */
@Composable
fun MessageCard(
    body: String,
    tone: MessageTone,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = tone.icon,
    action: (@Composable () -> Unit)? = null,
) {
    val status = CvPassTheme.statusColors
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        MessageTone.INFO -> colors.secondaryContainer to colors.onSecondaryContainer
        MessageTone.SUCCESS -> status.success to status.onSuccess
        MessageTone.PENDING -> status.pending to status.onPending
        MessageTone.ERROR -> colors.errorContainer to colors.onErrorContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                action?.invoke()
            }
        }
    }
}

private val MessageTone.icon: ImageVector
    get() = when (this) {
        MessageTone.INFO -> Icons.Outlined.Info
        MessageTone.SUCCESS -> Icons.Outlined.CheckCircle
        MessageTone.PENDING -> Icons.Outlined.HourglassTop
        MessageTone.ERROR -> Icons.Outlined.ErrorOutline
    }

/** Explains why NFC cannot be used and offers the settings shortcut when it is only off. */
@Composable
fun NfcUnavailableMessage(availability: NfcAvailability, unsupportedMessage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when (availability) {
        NfcAvailability.UNSUPPORTED -> MessageCard(
            title = stringResource(R.string.nfc_unsupported_title),
            body = unsupportedMessage,
            tone = MessageTone.ERROR,
            modifier = modifier,
        )

        NfcAvailability.DISABLED -> MessageCard(
            title = stringResource(R.string.nfc_disabled_title),
            body = stringResource(R.string.nfc_disabled_body),
            tone = MessageTone.PENDING,
            modifier = modifier,
            icon = Icons.Outlined.Nfc,
            action = {
                TextButton(onClick = { context.openNfcSettings() }) { Text(stringResource(R.string.nfc_open_settings)) }
            },
        )

        NfcAvailability.ENABLED -> Unit
    }
}

/**
 * Shows digits-only input with separators, such as `01012345678` as `010-1234-5678`,
 * while the underlying value stays plain digits.
 */
class FormattedDigitsTransformation(private val format: (String) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = format(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var digits = 0
                formatted.forEachIndexed { index, char ->
                    if (char.isDigit()) {
                        digits++
                        if (digits == offset) return index + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it.isDigit() }.coerceAtMost(text.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/** Formats dates and times in the user's locale and the device's time zone. */
class Formats(locale: Locale) {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)

    fun date(date: LocalDate): String = dateFormatter.format(date)

    fun time(instant: Instant): String = timeFormatter.format(instant.atZone(zone))

    fun dateTime(instant: Instant): String = dateTimeFormatter.format(instant.atZone(zone))
}

/** [Formats] for the current locale. */
@Composable
fun rememberFormats(): Formats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { Formats(locale) }
}
