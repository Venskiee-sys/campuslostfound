package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

private data class ClaimStep(
    val title: String,
    val time: String,
    val state: StepState
)

private enum class StepState { Done, Current, Pending }

/**
 * Claim Status: vertical timeline of the claim, pickup info and a way to
 * reach the finder.
 */
@Composable
fun ClaimStatusScreen(
    itemId: String = "",
    itemViewModel: ItemViewModel? = null,
    authViewModel: AuthViewModel? = null,
    chatViewModel: ChatViewModel? = null,
    onBackClick: () -> Unit = {},
    onMessageFinderClick: (String, String) -> Unit = { _, _ -> }
) {
    // Same anti-pattern as ClaimItemScreen: read the feeds, then fall back to
    // the on-demand fetch so a deep link resolves instead of staying blank.
    val lostItems = itemViewModel?.lostItems?.collectAsState()?.value
    val foundItems = itemViewModel?.foundItems?.collectAsState()?.value
    val myReports = itemViewModel?.myReports?.collectAsState()?.value
    val selectedItem = itemViewModel?.selectedItem?.collectAsState()?.value

    LaunchedEffect(itemId) { itemViewModel?.fetchItem(itemId) }

    val item = (lostItems.orEmpty() + foundItems.orEmpty() + myReports.orEmpty())
        .firstOrNull { it.id == itemId }
        ?: selectedItem?.takeIf { it.id == itemId }

    // There is no claims table yet, so the timeline must not pretend to know
    // steps that happened: the claim was submitted from this screen just now,
    // so only that step is done and only the review step is current.
    val submittedAt = remember(itemId) {
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    val steps = listOf(
        ClaimStep("Claim Submitted", submittedAt, StepState.Done),
        ClaimStep("Under Review", "Now", StepState.Current),
        ClaimStep("Verified", "Pending", StepState.Pending),
        ClaimStep("Ready for Pickup", "Pending", StepState.Pending),
        ClaimStep("Item Returned", "Pending", StepState.Pending)
    )

    val currentUser = authViewModel?.userData?.value
    val reporterId = item?.reportedBy.orEmpty()
    val reporterName = item?.reporterName.orEmpty()
    // Messaging only makes sense when the finder is someone else.
    val canMessageFinder = chatViewModel != null && currentUser != null &&
        reporterId.isNotBlank() && reporterId != currentUser.uid

    val startChat: () -> Unit = {
        val chat = chatViewModel
        val me = currentUser
        if (chat != null && me != null && reporterId.isNotBlank() && reporterId != me.uid) {
            chat.startChat(
                myId = me.uid,
                myName = me.fullName,
                otherId = reporterId,
                otherName = reporterName.ifBlank { "Finder" }
            ) { chatId ->
                onMessageFinderClick(chatId, reporterName.ifBlank { "Finder" })
            }
        }
    }

    if (item == null && itemViewModel?.itemLoading?.value == true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UniFindBackground)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = UniFindPrimary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Loading your claim…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFindTextSecondary
                )
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
                // Keep the last buttons clear of the gesture bar at full scroll
                // (the content ends with fixed spacers, not an inset).
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(
                title = "Claim Status",
                onBack = onBackClick,
                subtitle = "Track your claim"
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                // ---- Item card ----
                UniFindCard(cornerRadius = 22.dp) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // Narrow cards drop the tile size and park the status pill
                        // under the location so the item name keeps its space.
                        val roomy = maxWidth >= 300.dp
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconTile(
                                category = item?.category ?: "",
                                size = if (roomy) 60.dp else 52.dp,
                                cornerRadius = 18.dp
                            )
                            Spacer(Modifier.width(if (roomy) 14.dp else 10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                StatusTag(type = item?.type ?: "Found")
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = item?.name ?: "Your claimed item",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = UniFindText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = UniFindSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = item?.location ?: "Campus",
                                        color = UniFindTextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!roomy) {
                                    Spacer(Modifier.height(8.dp))
                                    StatusPill(label = "Under review")
                                }
                            }
                            if (roomy) StatusPill(label = "Under review")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---- Timeline ----
                UniFindCard(cornerRadius = 22.dp) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                        steps.forEachIndexed { index, step ->
                            TimelineRow(
                                step = step,
                                isLast = index == steps.lastIndex
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---- Pickup info ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(UniFindHighlight)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconTile(
                        icon = Icons.Outlined.LocationOn,
                        size = 40.dp,
                        cornerRadius = 12.dp,
                        iconSize = 20.dp,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pickup details",
                            style = MaterialTheme.typography.titleSmall,
                            color = UniFindText
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Pickup office: Security Office, Main Gate",
                            style = MaterialTheme.typography.bodySmall,
                            color = UniFindTextSecondary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Bring a valid ID — items are released only to the verified owner.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UniFindTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                PrimaryButton(
                    text = "Message Finder",
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    onClick = startChat,
                    enabled = canMessageFinder,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!canMessageFinder) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "The finder's conversation will appear here once your claim is under review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniFindTextSecondary
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun TimelineRow(step: ClaimStep, isLast: Boolean) {
    val circleColor = when (step.state) {
        StepState.Done -> UniFindPrimary
        StepState.Current -> UniFindWhite
        StepState.Pending -> UniFindSurfaceAlt
    }
    val lineColor = when (step.state) {
        StepState.Done -> UniFindPrimary
        StepState.Current -> UniFindMidGreen.copy(alpha = 0.5f)
        StepState.Pending -> UniFindOutline
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // time column
        Text(
            text = step.time,
            color = UniFindTextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.width(64.dp)
        )

        // circle + connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .then(
                        if (step.state == StepState.Current) Modifier.border(
                            width = 2.dp,
                            color = UniFindPrimary,
                            shape = CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (step.state) {
                    StepState.Done -> Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = UniFindOnPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    StepState.Current -> Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(UniFindPrimary)
                    )
                    StepState.Pending -> Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(UniFindOutline)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(lineColor)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                color = when (step.state) {
                    StepState.Pending -> UniFindTextTertiary
                    else -> UniFindText
                },
                fontWeight = if (step.state == StepState.Current) FontWeight.Bold else FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when (step.state) {
                    StepState.Done -> "Completed"
                    StepState.Current -> "In progress"
                    StepState.Pending -> "Waiting"
                },
                color = UniFindTextSecondary,
                fontSize = 12.sp
            )
            if (!isLast) Spacer(Modifier.height(14.dp))
        }
    }
}
