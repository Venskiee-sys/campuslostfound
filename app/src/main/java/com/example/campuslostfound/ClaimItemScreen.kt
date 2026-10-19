package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

/**
 * Claim Item: item summary, a pale-lime explainer box, the ownership questions
 * only the real owner can answer, proof upload and submit.
 */
@Composable
fun ClaimItemScreen(
    itemId: String = "",
    itemViewModel: ItemViewModel,
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    // React to the feeds + the on-demand fetch instead of reading them once:
    // `remember(itemId) { getItemById(...) }` froze whatever happened to be
    // cached at first composition, so the summary stayed blank on a deep link
    // and never updated when the item arrived.
    val lostItems by itemViewModel.lostItems.collectAsState()
    val foundItems by itemViewModel.foundItems.collectAsState()
    val myReports by itemViewModel.myReports.collectAsState()
    val selectedItem by itemViewModel.selectedItem.collectAsState()

    LaunchedEffect(itemId) { itemViewModel.fetchItem(itemId) }

    val item = (lostItems + foundItems + myReports).firstOrNull { it.id == itemId }
        ?: selectedItem?.takeIf { it.id == itemId }

    var insideColor by rememberSaveable { mutableStateOf("") }
    var cardInside by rememberSaveable { mutableStateOf("") }
    var marks by rememberSaveable { mutableStateOf("") }
    var hasProof by rememberSaveable { mutableStateOf(false) }

    if (item == null) {
        // Never drop out of composition without a way back: a cold start (or a
        // bad id) used to render an empty page.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UniFindBackground)
        ) {
            ScreenHeader(title = "Claim Item", onBack = onBackClick, subtitle = "Prove it's yours")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (itemViewModel.itemLoading.value) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = UniFindPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Loading item…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFindTextSecondary
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Item not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = UniFindText
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "It may have been removed or is no longer available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFindTextSecondary,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        SecondaryButton(text = "Go back", onClick = onBackClick)
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Insets applied before the scroll container so the viewport
                // itself shrinks: the keyboard never covers the focused field
                // and the last button can scroll clear of the gesture bar.
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = "Claim Item", onBack = onBackClick, subtitle = "Prove it's yours")

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                // ---- Item summary ----
                UniFindCard(cornerRadius = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconTile(
                            category = item?.category ?: "",
                            size = 60.dp,
                            cornerRadius = 18.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            StatusTag(type = item?.type ?: "Found")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = item?.name ?: "Selected item",
                                style = MaterialTheme.typography.titleMedium,
                                color = UniFindText
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Place,
                                    contentDescription = null,
                                    tint = UniFindSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = item?.location ?: "Campus",
                                    color = UniFindTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- Why we ask ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(UniFindHighlight)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconTile(
                        icon = Icons.Outlined.Shield,
                        size = 40.dp,
                        cornerRadius = 12.dp,
                        iconSize = 20.dp,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Only the owner will know these",
                            style = MaterialTheme.typography.titleSmall,
                            color = UniFindText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Answer these questions so we can verify the item belongs to you before the finder hands it over.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UniFindTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Ownership questions",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFindText
                )
                Spacer(Modifier.height(12.dp))

                ClaimQuestionField(
                    index = 1,
                    question = "What color is the inside?",
                    placeholder = "e.g. Brown leather",
                    value = insideColor,
                    onValueChange = { insideColor = it }
                )
                ClaimQuestionField(
                    index = 2,
                    question = "Is there a card inside? Which one?",
                    placeholder = "e.g. A student ID card",
                    value = cardInside,
                    onValueChange = { cardInside = it }
                )
                ClaimQuestionField(
                    index = 3,
                    question = "Any marks or damage?",
                    placeholder = "e.g. Scratched buckle on the left",
                    value = marks,
                    onValueChange = { marks = it }
                )

                Spacer(Modifier.height(8.dp))

                // ---- Proof of ownership (dashed) ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (hasProof) UniFindPaleLime.copy(alpha = 0.55f) else UniFindWhite
                        )
                        .dashedBorder(1.5.dp, UniFindOutline, RoundedCornerShape(18.dp))
                        .clickable { hasProof = !hasProof },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (hasProof) UniFindPrimary else UniFindTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (hasProof) "Proof attached (1)" else "Add proof of ownership",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (hasProof) UniFindPrimary else UniFindTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                PrimaryButton(
                    text = "Submit Claim",
                    onClick = onSubmitClick,
                    enabled = insideColor.isNotBlank() && cardInside.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ClaimQuestionField(
    index: Int,
    question: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.labelMedium,
                color = UniFindSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.labelLarge,
                color = UniFindText
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = UniFindTextTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = UniFindWhite,
                unfocusedContainerColor = UniFindSurfaceAlt,
                disabledContainerColor = UniFindSurfaceAlt,
                focusedBorderColor = UniFindPrimary,
                unfocusedBorderColor = UniFindOutline,
                cursorColor = UniFindPrimary,
                focusedTextColor = UniFindText,
                unfocusedTextColor = UniFindText
            )
        )
    }
}

/** Dashed rounded outline drawn on top of the receiver. */
private fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: RoundedCornerShape,
    radius: Dp = 18.dp
): Modifier =
    clip(shape).drawBehind {
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        )
        val inset = stroke.width / 2f
        val r = radius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke.width, size.height - stroke.width),
            cornerRadius = CornerRadius(r, r),
            style = stroke
        )
    }
