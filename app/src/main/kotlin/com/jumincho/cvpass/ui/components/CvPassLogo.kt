package com.jumincho.cvpass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jumincho.cvpass.R
import com.jumincho.cvpass.ui.theme.BrandColors

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
