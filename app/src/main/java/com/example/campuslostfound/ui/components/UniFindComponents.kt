package com.example.campuslostfound.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.ui.theme.*

// ===========================================================================
// Palette shortcuts (full palette lives in ui.theme.Color)
// ===========================================================================
/**
 * Design-system palette. Neutrals (background, surface, text, …) follow the
 * active light/dark mode; brand accents stay constant.
 */
object UniFind {
    val Primary: Color get() = UniFindPrimary
    val Secondary: Color get() = UniFindSecondary
    val MidGreen: Color get() = UniFindMidGreen
    val Lime: Color get() = UniFindLime
    val PaleLime: Color get() = UniFindPaleLime
    val Background: Color get() = UniFindBackground
    val Text: Color get() = UniFindText
    val TextSecondary: Color get() = UniFindTextSecondary
    val TextTertiary: Color get() = UniFindTextTertiary
    val White: Color get() = UniFindWhite
    val Surface: Color get() = UniFindSurface
    val SurfaceAlt: Color get() = UniFindSurfaceAlt
    val Divider: Color get() = UniFindDivider
    val Outline: Color get() = UniFindOutline
    /** Soft highlight panel (info boxes, unread rows, avatars). */
    val Highlight: Color get() = UniFindHighlight
    /** Always white — for text/icons painted on brand greens. */
    val OnPrimary: Color get() = UniFindOnPrimary
    val LostText: Color get() = LostTagText
    val LostBackground: Color get() = LostTagBackground
    val FoundText: Color get() = FoundTagText
    val FoundBackground: Color get() = FoundTagBackground
}

// ===========================================================================
// Category icons + soft tiles (line icons on soft colored tiles, no photos)
// ===========================================================================

private val CategoryIconMap = mapOf(
    "wallets" to Icons.Outlined.AccountBalanceWallet,
    "wallet" to Icons.Outlined.AccountBalanceWallet,
    "gadgets" to Icons.Outlined.Smartphone,
    "gadget" to Icons.Outlined.Smartphone,
    "phones" to Icons.Outlined.Smartphone,
    "phone" to Icons.Outlined.Smartphone,
    "electronics" to Icons.Outlined.Devices,
    "laptop" to Icons.Outlined.Devices,
    "laptops" to Icons.Outlined.Devices,
    "charger" to Icons.Outlined.BatteryChargingFull,
    "chargers" to Icons.Outlined.BatteryChargingFull,
    "cables" to Icons.Outlined.BatteryChargingFull,
    "keys" to Icons.Outlined.Key,
    "key" to Icons.Outlined.Key,
    "bags" to Icons.Outlined.Backpack,
    "bag" to Icons.Outlined.Backpack,
    "backpack" to Icons.Outlined.Backpack,
    "backpacks" to Icons.Outlined.Backpack,
    "ids" to Icons.Outlined.Badge,
    "id" to Icons.Outlined.Badge,
    "id card" to Icons.Outlined.Badge,
    "documents" to Icons.Outlined.Description,
    "document" to Icons.Outlined.Description,
    "books" to Icons.AutoMirrored.Outlined.MenuBook,
    "book" to Icons.AutoMirrored.Outlined.MenuBook,
    "clothing" to Icons.Outlined.Checkroom,
    "clothes" to Icons.Outlined.Checkroom,
    "umbrella" to Icons.Outlined.Umbrella,
    "umbrellas" to Icons.Outlined.Umbrella,
    "water bottle" to Icons.Outlined.WaterDrop,
    "bottle" to Icons.Outlined.WaterDrop,
    "pets" to Icons.Outlined.Pets,
    "pet" to Icons.Outlined.Pets
)

private val CategoryTileColors = mapOf(
    "wallets" to TilePaleLime,
    "wallet" to TilePaleLime,
    "gadgets" to TileSoftMint,
    "gadget" to TileSoftMint,
    "phones" to TileSoftMint,
    "phone" to TileSoftMint,
    "electronics" to TileSoftMint,
    "laptop" to TileSoftMint,
    "laptops" to TileSoftMint,
    "charger" to TileSoftOlive,
    "chargers" to TileSoftOlive,
    "cables" to TileSoftOlive,
    "keys" to TileSoftSage,
    "key" to TileSoftSage,
    "bags" to TileSoftGreen,
    "bag" to TileSoftGreen,
    "backpack" to TileSoftGreen,
    "backpacks" to TileSoftGreen,
    "ids" to TileSoftOlive,
    "id" to TileSoftOlive,
    "id card" to TileSoftOlive,
    "documents" to TileSoftSage,
    "document" to TileSoftSage,
    "books" to TileSoftGreen,
    "book" to TileSoftGreen,
    "clothing" to TilePaleLime,
    "clothes" to TilePaleLime,
    "umbrella" to TileSoftMint,
    "umbrellas" to TileSoftMint,
    "water bottle" to TileSoftSage,
    "bottle" to TileSoftSage,
    "pets" to TileSoftGreen,
    "pet" to TileSoftGreen
)

/** Line icon for a category name (falls back to a generic package icon). */
fun categoryIcon(category: String): ImageVector =
    CategoryIconMap[category.trim().lowercase()] ?: Icons.Outlined.Inventory2

/** Soft tile background for a category name. */
fun categoryTileColor(category: String): Color =
    CategoryTileColors[category.trim().lowercase()] ?: TileSoftSage

/** Line icon for a status: LOST items get a search-alert icon, FOUND a check. */
fun statusIcon(type: String): ImageVector =
    if (type.trim().equals("Lost", ignoreCase = true)) Icons.AutoMirrored.Outlined.Help
    else Icons.Outlined.Inventory2

/** A soft rounded tile with a line icon — the UniFind item thumbnail. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    containerColor: Color = TileSoftSage,
    iconTint: Color = UniFindPrimary,
    iconSize: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

/** Category thumbnail: line icon on its soft colored tile. */
@Composable
fun CategoryIconTile(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 16.dp
) {
    IconTile(
        icon = categoryIcon(category),
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius,
        containerColor = categoryTileColor(category)
    )
}

// ===========================================================================
// Status tags — orange ONLY for LOST, green-on-pale-lime for FOUND
// ===========================================================================

/** The LOST / FOUND pill used across cards, details and lists. */
@Composable
fun StatusTag(type: String, modifier: Modifier = Modifier) {
    val isLost = type.trim().equals("Lost", ignoreCase = true) ||
        type.trim().equals("LOST", ignoreCase = true)
    val label = if (isLost) "LOST" else "FOUND"
    val bg = if (isLost) LostTagBackground else FoundTagBackground
    val fg = if (isLost) LostTagText else FoundTagText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

/** Soft status pill (Searching, Possible match, Claimed, Returned, …). */
@Composable
fun StatusPill(label: String, modifier: Modifier = Modifier) {
    // Computed per composition so the pill follows light/dark mode.
    val (bg, fg) = when (label.trim().lowercase()) {
        "searching", "reported", "open" -> TileSoftSage to UniFindPrimary
        "possible match" -> LostTagBackground to LostTagText
        "under review" -> TilePaleLime to UniFindText
        "claimed" -> TileSoftGreen to UniFindSecondary
        "ready for pickup", "returned", "resolved" -> UniFindPaleLime to UniFindPrimary
        "closed" -> UniFindSurfaceAlt to UniFindTextSecondary
        else -> UniFindSurfaceAlt to UniFindTextSecondary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===========================================================================
// Buttons
// ===========================================================================

/** Full-width dark green button with 18dp corners. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = UniFindPrimary,
            contentColor = UniFindOnPrimary,
            disabledContainerColor = UniFindMidGreen.copy(alpha = 0.5f),
            disabledContentColor = UniFindOnPrimary.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Outlined (Google-style) secondary button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = UniFindText
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===========================================================================
// Chips
// ===========================================================================

@Composable
fun UniFindChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    val bg = if (selected) UniFindPrimary else UniFindWhite
    val fg = if (selected) UniFindOnPrimary else UniFindText
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) UniFindPrimary else UniFindOutline,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (badge > 0) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (selected) UniFindLime else UniFindPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badge.toString(),
                    color = if (selected) UniFindPrimary else UniFindOnPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Horizontally scrollable row of filter chips (All / Lost / Found / …). */
@Composable
fun FilterChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<String, Int> = emptyMap()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            UniFindChip(
                label = option,
                selected = option == selected,
                onClick = { onSelect(option) },
                badge = badges[option] ?: 0
            )
        }
    }
}

// ===========================================================================
// Search bar
// ===========================================================================

/** White rounded search field (16dp corners) with a line search icon. */
@Composable
fun UniFindSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search items...",
    onClick: (() -> Unit)? = null
) {
    val compact = rememberIsLandscape()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UniFindWhite)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = UniFindTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            if (onClick != null) {
                Text(
                    value.ifBlank { placeholder },
                    color = if (value.isBlank()) UniFindTextTertiary else UniFindText,
                    fontSize = 15.sp
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isBlank()) {
                        Text(placeholder, color = UniFindTextTertiary, fontSize = 15.sp)
                    }
                    BasicSearchInput(value = value, onValueChange = onValueChange)
                }
            }
        }
    }
}

@Composable
private fun BasicSearchInput(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = UniFindText,
            fontSize = 15.sp,
            fontFamily = PlusJakartaSans
        ),
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(UniFindPrimary),
        modifier = Modifier.fillMaxWidth()
    )
}

// ===========================================================================
// Input fields
// ===========================================================================

/**
 * UniFind form field with the reference text layout: a small uppercase
 * semibold label sits above a larger placeholder inside the same rounded
 * container. Keeps the auth screens' existing look — 16dp corners, soft fill,
 * outline that turns green (border, label and leading icon) while focused,
 * 24dp leading icon and an optional trailing slot (password eye).
 */
@Composable
fun UniFindTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconDescription: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    val borderColor = if (focused) UniFindPrimary else UniFindOutline
    val containerColor = if (focused) UniFindWhite else UniFindSurfaceAlt
    val accentColor = if (focused) UniFindPrimary else UniFindTextSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
            }
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = leadingIconDescription,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = UniFindTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = UniFindText
                        ),
                        cursorBrush = SolidColor(UniFindPrimary),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

// ===========================================================================
// Cards / sections
// ===========================================================================

/** White rounded card (20dp) with a soft shadow. */
@Composable
fun UniFindCard(
    modifier: Modifier = Modifier,
    containerColor: Color = UniFindWhite,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(content = content)
    }
}

/** "Recent posts" style heading with an optional right-aligned action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = UniFindText,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = UniFindSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// ===========================================================================
// Item cards
// ===========================================================================

/** Two-column grid card: icon tile + tag + name + location + time. */
@Composable
fun ItemGridCard(
    name: String,
    type: String,
    location: String,
    timePosted: String,
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UniFindCard(modifier = modifier.clickable(onClick = onClick), cornerRadius = 20.dp) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Narrow cards lift the status tag above the icon row and shrink the
            // tile so the item name keeps usable space instead of ellipsising.
            val compact = maxWidth < 170.dp
            Column(modifier = Modifier.padding(14.dp)) {
                if (compact) {
                    StatusTag(type = type)
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.Top) {
                    CategoryIconTile(
                        category = category,
                        size = if (compact) 44.dp else 52.dp,
                        cornerRadius = 14.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (!compact) {
                            StatusTag(type = type)
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            color = UniFindText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = UniFindSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location,
                        color = UniFindTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(timePosted, color = UniFindTextTertiary, fontSize = 11.sp)
            }
        }
    }
}

/** Horizontal list row: icon tile + name, location, date + tag/status. */
@Composable
fun ItemRowCard(
    name: String,
    type: String,
    location: String,
    date: String,
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusLabel: String? = null
) {
    UniFindCard(modifier = modifier.clickable(onClick = onClick), cornerRadius = 20.dp) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Under ~300dp of card width the chip moves under the text and the
            // icon shrinks, so the name/location never collapse to a sliver.
            val roomy = maxWidth >= 300.dp
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconTile(
                    category = category,
                    size = if (roomy) 54.dp else 44.dp,
                    cornerRadius = 16.dp
                )
                Spacer(Modifier.width(if (roomy) 14.dp else 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        color = UniFindText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = UniFindSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            location,
                            color = UniFindTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(date, color = UniFindTextTertiary, fontSize = 11.sp)
                        if (!roomy) {
                            Spacer(Modifier.width(8.dp))
                            if (statusLabel != null) StatusPill(statusLabel) else StatusTag(type)
                        }
                    }
                }
                if (roomy) {
                    Spacer(Modifier.width(10.dp))
                    if (statusLabel != null) StatusPill(statusLabel) else StatusTag(type)
                }
            }
        }
    }
}

// ===========================================================================
// Headers
// ===========================================================================

/** Dark green header block with rounded bottom corners. */
@Composable
fun GreenHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // The green runs behind the status bar in every theme, so the icons must
    // be light regardless of the system dark/light setting.
    SetStatusBarIconAppearance(lightIcons = true)
    val compact = rememberIsLandscape()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(
                bottomStart = if (compact) 16.dp else 28.dp,
                bottomEnd = if (compact) 16.dp else 28.dp
            ))
            .background(UniFindPrimary)
            // Inset-aware: the green still fills the status bar, but the exact bar
            // height varies by device, so it is measured instead of hard-coded.
            .statusBarsPadding()
            .padding(
                start = 20.dp, end = 20.dp,
                top = if (compact) 8.dp else 14.dp,
                bottom = if (compact) 12.dp else 24.dp
            )
    ) {
        Column(content = content)
    }
}

/** Back button in a soft white circle, for green or plain headers. */
@Composable
fun CircleBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = UniFindText
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(UniFindWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** White top bar with a large title (and optional back / action slots). */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Pale header: keep the icons dark even when the system theme is dark.
    SetStatusBarIconAppearance(lightIcons = false)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 20.dp, end = 20.dp,
                top = if (rememberIsLandscape()) 6.dp else 12.dp,
                bottom = if (rememberIsLandscape()) 4.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleBackButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = UniFindText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(subtitle, color = UniFindTextSecondary, fontSize = 13.sp)
            }
        }
        Row(content = actions)
    }
}

// ===========================================================================
// Bottom navigation — Home, Reports, raised "+", Messages, Alerts
// ===========================================================================

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(
            badge = {
                if (badge > 0) {
                    Badge(
                        containerColor = LostTagText,
                        contentColor = UniFindOnPrimary,
                        modifier = Modifier.offset(x = 6.dp, y = (-2).dp)
                    ) { Text(if (badge > 9) "9+" else badge.toString()) }
                }
            }
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) UniFindPrimary else UniFindTextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) UniFindPrimary else UniFindTextTertiary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * UniFind bottom bar: Home, Reports, a raised round green "+" for reporting,
 * Messages and Alerts.
 */
@Composable
fun UniFindBottomBar(
    current: String,
    onHome: () -> Unit,
    onReports: () -> Unit,
    onReportClick: () -> Unit,
    onMessages: () -> Unit,
    onAlerts: () -> Unit,
    modifier: Modifier = Modifier,
    messageBadge: Int = 0,
    alertBadge: Int = 0
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = UniFindWhite,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem("Home", Icons.Outlined.Home, current == "home", 0, onHome)
                BottomNavItem("Reports", Icons.AutoMirrored.Outlined.Assignment, current == "reports", 0, onReports)
                Spacer(Modifier.size(64.dp))
                BottomNavItem("Messages", Icons.AutoMirrored.Outlined.Chat, current == "messages", messageBadge, onMessages)
                BottomNavItem("Alerts", Icons.Outlined.Notifications, current == "alerts", alertBadge, onAlerts)
            }
        }

        // Raised round green "+" button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(UniFindPrimary)
                .border(width = 5.dp, color = UniFindBackground, shape = CircleShape)
                .clickable(onClick = onReportClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Report an item",
                tint = UniFindOnPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

// ===========================================================================
// Small helpers
// ===========================================================================

/**
 * Responsive wrapper around [UniFindBottomBar]: on phones/large phones/tablets
 * it renders the mobile bottom bar; on laptops/desktop-width windows it turns
 * the same five destinations into a horizontal tab row so wide screens get a
 * desktop-style navigation instead of a stretched bottom bar.
 */
@Composable
fun UniFindNavBar(
    current: String,
    onHome: () -> Unit,
    onReports: () -> Unit,
    onReportClick: () -> Unit,
    onMessages: () -> Unit,
    onAlerts: () -> Unit,
    modifier: Modifier = Modifier,
    messageBadge: Int = 0,
    alertBadge: Int = 0
) {
    if (rememberWindowWidth() == WindowWidth.EXPANDED) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = UniFindWhite,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem("Home", Icons.Outlined.Home, current == "home", 0, onHome)
                BottomNavItem("Reports", Icons.AutoMirrored.Outlined.Assignment, current == "reports", 0, onReports)
                BottomNavItem("Messages", Icons.AutoMirrored.Outlined.Chat, current == "messages", messageBadge, onMessages)
                BottomNavItem("Alerts", Icons.Outlined.Notifications, current == "alerts", alertBadge, onAlerts)
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    text = "Add Report",
                    onClick = onReportClick,
                    modifier = Modifier.widthIn(min = 140.dp)
                )
            }
        }
    } else {
        UniFindBottomBar(
            current = current,
            onHome = onHome,
            onReports = onReports,
            onReportClick = onReportClick,
            onMessages = onMessages,
            onAlerts = onAlerts,
            modifier = modifier,
            messageBadge = messageBadge,
            alertBadge = alertBadge
        )
    }
}

/**
 * Landscape navigation: same five destinations as [UniFindBottomBar], but
 * docked as a slim rail on the left so the vertical space goes to content
 * instead of a raised bottom bar + FAB overlapping the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniFindNavRail(
    current: String,
    onHome: () -> Unit,
    onReports: () -> Unit,
    onReportClick: () -> Unit,
    onMessages: () -> Unit,
    onAlerts: () -> Unit,
    modifier: Modifier = Modifier,
    messageBadge: Int = 0,
    alertBadge: Int = 0
) {
    Surface(
        modifier = modifier.width(80.dp),
        // Same color as the page background on purpose: without a shadow or
        // contrasting surface, the rail reads as a plain column of tabs
        // instead of a boxed panel with a hard edge next to the content.
        color = UniFindBackground,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            NavigationRailItem(
                selected = current == "home",
                onClick = onHome,
                icon = {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Home", fontSize = 11.sp) }
            )
            NavigationRailItem(
                selected = current == "reports",
                onClick = onReports,
                icon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = "Reports",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Reports", fontSize = 11.sp) }
            )
            NavigationRailItem(
                selected = false,
                onClick = onReportClick,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(UniFindPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Report an item",
                            tint = UniFindOnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = { Text("Report", fontSize = 11.sp) }
            )
            NavigationRailItem(
                selected = current == "messages",
                onClick = onMessages,
                icon = {
                    BadgedBox(badge = {
                        if (messageBadge > 0) {
                            Badge(
                                containerColor = LostTagText,
                                contentColor = UniFindOnPrimary
                            ) { Text(if (messageBadge > 9) "9+" else messageBadge.toString()) }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Messages",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text("Messages", fontSize = 11.sp) }
            )
            NavigationRailItem(
                selected = current == "alerts",
                onClick = onAlerts,
                icon = {
                    BadgedBox(badge = {
                        if (alertBadge > 0) {
                            Badge(
                                containerColor = LostTagText,
                                contentColor = UniFindOnPrimary
                            ) { Text(if (alertBadge > 9) "9+" else alertBadge.toString()) }
                        }
                    }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Alerts",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text("Alerts", fontSize = 11.sp) }
            )
        }
    }
}

/**
 * The "+" report chooser, shared so the global bottom bar in MainActivity can
 * use the same design the Home screen previously kept.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReportChoiceSheet(
    onDismissRequest: () -> Unit,
    onLost: () -> Unit,
    onFound: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = UniFindWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text("Report an item", style = MaterialTheme.typography.titleLarge, color = UniFindText)
            Spacer(Modifier.height(6.dp))
            Text(
                "What would you like to report?",
                style = MaterialTheme.typography.bodyMedium,
                color = UniFindTextSecondary
            )
            Spacer(Modifier.height(18.dp))
            ReportChoiceOption(
                title = "I lost something",
                message = "Tell the campus what you lost",
                icon = statusIcon("Lost"),
                containerColor = UniFind.LostBackground,
                iconTint = UniFind.LostText,
                onClick = onLost
            )
            Spacer(Modifier.height(12.dp))
            ReportChoiceOption(
                title = "I found something",
                message = "Help return an item to its owner",
                icon = statusIcon("Found"),
                containerColor = UniFind.FoundBackground,
                iconTint = UniFind.FoundText,
                onClick = onFound
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ReportChoiceOption(
    title: String,
    message: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    UniFindCard(
        containerColor = UniFindSurfaceAlt,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                size = 48.dp,
                cornerRadius = 14.dp,
                containerColor = containerColor,
                iconTint = iconTint,
                iconSize = 24.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = UniFindText)
                Spacer(Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = UniFindTextSecondary)
            }
        }
    }
}

/** Empty-state block for lists. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(TilePaleLime),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = UniFindPrimary, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = UniFindText)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = UniFindTextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Label/value info row with a soft icon (Item Details, Claim screens). */
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, size = 38.dp, cornerRadius = 12.dp, iconSize = 19.dp)
        Spacer(Modifier.width(12.dp))
        // Weighted so a long value wraps inside the row instead of being
        // measured against the full card width and pushed past its edge.
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = UniFindTextTertiary, fontSize = 12.sp, maxLines = 1)
            Text(
                value,
                color = UniFindText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "12m ago" / "3h ago" / "2d ago" style relative time from an epoch millis. */
fun timeAgo(timestamp: Long): String {
    if (timestamp <= 0L) return "just now"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
