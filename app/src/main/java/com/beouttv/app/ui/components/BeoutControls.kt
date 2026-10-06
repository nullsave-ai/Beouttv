package com.beouttv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable

@Composable
fun BeoutIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BeoutColors.TextPrimary
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(BeoutDimensions.IconSize))
    }
}

@Composable
fun BeoutPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = BeoutShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = BeoutColors.PrimaryBlue,
            contentColor = BeoutColors.TextPrimary
        )
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(BeoutDimensions.IconSizeSmall))
            Spacer(Modifier.width(BeoutDimensions.SpaceXS))
        }
        Text(text, style = BeoutTextStyles.Button)
    }
}

@Composable
fun BeoutSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = BeoutShapes.Medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BeoutColors.TextPrimary)
    ) {
        Text(text, style = BeoutTextStyles.Button)
    }
}

@Composable
fun BeoutSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isTvMode by com.beouttv.app.core.tv.DeviceTypeDetector.isTvModeFlow.collectAsState()
    val startPadding = if (isTvMode) 84.dp else BeoutDimensions.ScreenPadding
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = BeoutDimensions.ScreenPadding, top = BeoutDimensions.SpaceXS, bottom = BeoutDimensions.SpaceXS),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = BeoutTextStyles.SectionTitle, color = BeoutColors.TextPrimary)
        onSeeAllClick?.let { onClick ->
            Text(
                com.beouttv.app.data.i18n.S.seeAll(),
                style = BeoutTextStyles.SecondaryInfo,
                color = BeoutColors.BrightBlue,
                modifier = Modifier.clickable {
                    if (!isTvMode) {
                        com.beouttv.app.data.ads.AdsterraManager.onSeeAllClicked(context)
                    }
                    onClick()
                }
            )
        }
    }
}

@Composable
fun BeoutCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) BeoutColors.PrimaryBlue else BeoutColors.Card
    val textColor = if (selected) BeoutColors.TextPrimary else BeoutColors.TextSecondary

    Box(
        modifier = Modifier
            .clip(BeoutShapes.Large)
            .background(background)
            .tvFocusable(shape = BeoutShapes.Large)
            .clickable(onClick = onClick)
            .padding(horizontal = BeoutDimensions.SpaceM, vertical = BeoutDimensions.SpaceXS)
    ) {
        Text(label, style = BeoutTextStyles.SecondaryInfo, color = textColor)
    }
}

@Composable
fun BeoutCategoryRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isTvMode by com.beouttv.app.core.tv.DeviceTypeDetector.isTvModeFlow.collectAsState()
    LazyRow(
        contentPadding = PaddingValues(
            start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding,
            end = BeoutDimensions.ScreenPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceXS)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            BeoutCategoryChip(
                label = category,
                selected = category == selected,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
fun BeoutSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: (() -> Unit)? = null
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .padding(start = BeoutDimensions.SpaceM, end = BeoutDimensions.SpaceS, top = 6.dp, bottom = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = BeoutColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(BeoutDimensions.SpaceS))
        BasicTextFieldPlaceholder(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            onSearch = {
                keyboardController?.hide()
                onSearch?.invoke()
            },
            modifier = Modifier.weight(1f)
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Clear,
                contentDescription = "Clear",
                tint = BeoutColors.TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(2.dp)
            )
            Spacer(Modifier.width(8.dp))
            // Search Submit / Confirmation Button ("زر الموافقة")
            Box(
                modifier = Modifier
                    .clip(BeoutShapes.Small)
                    .background(BeoutColors.BrightBlue)
                    .clickable {
                        keyboardController?.hide()
                        onSearch?.invoke()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "تأكيد البحث",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = com.beouttv.app.data.i18n.tr("بحث", "Search", "Ara", "جستجو", "Chercher"),
                        style = BeoutTextStyles.Caption.copy(
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicTextFieldPlaceholder(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch?.invoke() }
        ),
        textStyle = BeoutTextStyles.SecondaryInfo.copy(color = BeoutColors.TextPrimary),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(BeoutColors.BrightBlue),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(placeholder, style = BeoutTextStyles.SecondaryInfo, color = BeoutColors.TextDisabled)
            }
            innerTextField()
        }
    )
}

/** Small circular avatar/logo placeholder used where a real image is unavailable. */
@Composable
fun BeoutLogoPlaceholder(initial: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BeoutColors.ElevatedSurface),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            initial.take(1).uppercase(),
            style = BeoutTextStyles.CardTitle,
            color = BeoutColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Circular floating action button used for navigation, favorites, sharing, and overlay controls.
 */
@Composable
fun FloatingCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = BeoutColors.TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(modifier)
            .tvFocusable(shape = CircleShape, onEnter = onClick)
            .clip(CircleShape)
            .background(BeoutColors.Surface.copy(alpha = 0.8f))
            .border(1.dp, BeoutColors.Divider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

