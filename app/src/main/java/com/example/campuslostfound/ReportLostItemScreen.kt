package com.example.campuslostfound

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLostItemScreen(
    authViewModel: AuthViewModel,
    itemViewModel: ItemViewModel,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onSwitchTypeClick: () -> Unit = {},
    onPickLocationClick: () -> Unit = {},
    pickedLocation: String? = null,
    onLocationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    // rememberSaveable: rotating the device (or coming back from the map picker)
    // used to wipe the whole form while the date picker survived.
    var itemName by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var dateLost by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    // Category Selection State
    val categories = listOf("Bags", "Electronics", "Documents", "Personal Items", "Others")
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var otherCategory by rememberSaveable { mutableStateOf("") }

    // Photo State (stored as a string so it survives state restoration)
    var imageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val imageUri = imageUriString?.let(Uri::parse)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUriString = uri?.toString() }

    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading = itemViewModel.isLoading.value
    val firebaseError = itemViewModel.errorMessage.value

    // A location chosen on the map screen.
    LaunchedEffect(pickedLocation) {
        if (!pickedLocation.isNullOrBlank()) {
            location = pickedLocation
            onLocationConsumed()
        }
    }

    // Date Picker State — pre-selected so tapping OK without picking a day
    // doesn't silently do nothing.
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateLost = formatPickerDate(it)
                        localErrorMessage = null
                    }
                    showDatePicker = false
                }) { Text("OK", color = UniFindPrimary) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCEL", color = UniFindTextSecondary) } }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(Unit) { itemViewModel.clearError() }

    val formIsValid = itemName.isNotBlank() &&
        selectedCategory.isNotBlank() &&
        (selectedCategory != "Others" || otherCategory.isNotBlank()) &&
        location.isNotBlank() &&
        dateLost.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
            // Lifts the whole form — including the pinned submit bar — above the
            // keyboard, so typing in the lower fields never hides "Post Report".
            .imePadding()
    ) {
        // ------------------------------------------------------------------
        // Top bar: back + title
        // ------------------------------------------------------------------
        ScreenHeader(
            title = "Report an Item",
            onBack = onBackClick,
            subtitle = "Tell us what you lost",
            actions = { StatusTag(type = "Lost") }
        )

        // ------------------------------------------------------------------
        // Segmented lost / found toggle — active side is dark green
        // ------------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(UniFindWhite)
                .border(1.dp, UniFindOutline, RoundedCornerShape(16.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UniFindPrimary)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "I lost something",
                    color = UniFindOnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSwitchTypeClick)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "I found something",
                    color = UniFindText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ------------------------------------------------------------------
        // Scrolling form
        // ------------------------------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Dashed photo upload box
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUriString = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UniFindWhite)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove Photo",
                            tint = UniFindText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(UniFindHighlight)
                        .drawBehind {
                            val inset = 1.dp.toPx()
                            drawRoundRect(
                                color = UniFindOutline,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2, size.height - inset * 2),
                                cornerRadius = CornerRadius(17.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(14f, 10f),
                                        0f
                                    )
                                )
                            )
                        }
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(UniFindWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                contentDescription = "Add photos",
                                tint = UniFindPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Add a photo",
                            style = MaterialTheme.typography.labelLarge,
                            color = UniFindText
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "PNG or JPG",
                            style = MaterialTheme.typography.labelSmall,
                            color = UniFindTextTertiary
                        )
                    }
                }
            }

            // Item name
            ReportLostField(
                value = itemName,
                onValueChange = { itemName = it; localErrorMessage = null },
                label = "Item Name",
                placeholder = "e.g. Black Backpack",
                leadingIcon = Icons.Outlined.Inventory2
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    // PrimaryNotEditable: the field is readOnly and only opens
                    // the menu — the parameterless menuAnchor() overload that
                    // this replaced is deprecated.
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = reportLostFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedCategory = selectionOption
                                localErrorMessage = null
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            if (selectedCategory == "Others") {
                ReportLostField(
                    value = otherCategory,
                    onValueChange = { otherCategory = it; localErrorMessage = null },
                    label = "Please specify category",
                    placeholder = "e.g. Student ID",
                    leadingIcon = Icons.Outlined.Edit
                )
            }

            // Description
            ReportLostField(
                value = description,
                onValueChange = { description = it; localErrorMessage = null },
                label = "Description",
                placeholder = "Describe the item, color, brand, or anything that can help identify it.",
                leadingIcon = Icons.Outlined.Description,
                // Grows with the typed text (up to maxLines) instead of clipping it
                // at large font scales.
                modifier = Modifier.heightIn(min = 140.dp, max = 240.dp),
                multiline = true
            )

            // Location + Change link
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReportLostField(
                    value = location,
                    onValueChange = { location = it; localErrorMessage = null },
                    label = "Location Lost",
                    placeholder = "e.g. Library",
                    leadingIcon = Icons.Outlined.LocationOn,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Change",
                    color = UniFindSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onPickLocationClick)
                        .padding(horizontal = 6.dp, vertical = 14.dp)
                )
            }

            // Date
            OutlinedTextField(
                value = dateLost,
                onValueChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                enabled = false,
                label = { Text("Date Lost") },
                placeholder = { Text("e.g. September 3, 2026") },
                leadingIcon = {
                    Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    // A real button: TalkBack can activate it, and it always opens
                    // the picker even where the disabled field swallows taps.
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = "Pick date lost",
                            modifier = Modifier.size(20.dp),
                            tint = UniFindPrimary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = reportLostFieldColors()
            )

            Spacer(Modifier.height(8.dp))
        }

        // ------------------------------------------------------------------
        // Pinned submit button
        // ------------------------------------------------------------------
        Surface(
            color = UniFindWhite,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val displayError = localErrorMessage ?: firebaseError
                displayError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(UniFindPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = UniFindOnPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    PrimaryButton(
                        text = "Post Report",
                        onClick = {
                            val finalCategory = if (selectedCategory == "Others") otherCategory else selectedCategory
                            val user = authViewModel.userData.value
                            if (user != null) {
                                itemViewModel.reportItem(
                                    name = itemName,
                                    category = finalCategory,
                                    location = location,
                                    date = dateLost,
                                    description = description,
                                    type = "Lost",
                                    userId = user.uid,
                                    userName = user.fullName,
                                    imageUri = imageUri,
                                    context = context,
                                    onSuccess = onSubmitClick
                                )
                            } else { localErrorMessage = "User data not loaded" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = formIsValid
                    )
                }
            }
        }
    }
}

/**
 * Formats the date picker's selection.
 *
 * The picker returns midnight **in UTC**, so it has to be formatted in UTC too:
 * formatting it with the device timezone rendered the previous day for anyone
 * west of Greenwich, and that wrong string was then saved as the report's date.
 */
internal fun formatPickerDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

/** Shared UniFind field styling: soft `SurfaceAlt` fill, 16dp corners. */
@Composable
private fun reportLostFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = UniFindSurfaceAlt,
        unfocusedContainerColor = UniFindSurfaceAlt,
        disabledContainerColor = UniFindSurfaceAlt,
        focusedTextColor = UniFindText,
        unfocusedTextColor = UniFindText,
        disabledTextColor = UniFindText,
        cursorColor = UniFindPrimary,
        focusedBorderColor = UniFindPrimary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        focusedLabelColor = UniFindPrimary,
        unfocusedLabelColor = UniFindTextSecondary,
        disabledLabelColor = UniFindTextSecondary,
        focusedPlaceholderColor = UniFindTextTertiary,
        unfocusedPlaceholderColor = UniFindTextTertiary,
        disabledPlaceholderColor = UniFindTextTertiary,
        focusedLeadingIconColor = UniFindPrimary,
        unfocusedLeadingIconColor = UniFindTextSecondary,
        disabledLeadingIconColor = UniFindTextSecondary,
        focusedTrailingIconColor = UniFindPrimary,
        unfocusedTrailingIconColor = UniFindTextSecondary
    )

@Composable
private fun ReportLostField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    multiline: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        singleLine = !multiline,
        maxLines = if (multiline) 6 else 1,
        shape = RoundedCornerShape(16.dp),
        colors = reportLostFieldColors()
    )
}
