package com.example.demo_musicsound.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.demo_musicsound.R
import com.example.demo_musicsound.community.UserProfile
import com.example.demo_musicsound.ui.util.LocaleUtils
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMenuDialog(
    profile: UserProfile?,
    emailFallback: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var currentLang by remember { mutableStateOf(LocaleUtils.getCurrentLanguage(context)) }
    var expanded by remember { mutableStateOf(false) }

    val email = (profile?.email?.ifBlank { emailFallback } ?: emailFallback).ifBlank { "—" }

    val languageOptions = listOf(
        "en" to "English",
        "it" to "Italiano",
        "fr" to "Français"
    )

    fun langLabel(code: String): String =
        languageOptions.firstOrNull { it.first == code }?.second ?: "English"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // ✅ più alto, così non “taglia” la card Language
                .heightIn(max = 620.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = GraySurface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                // ✅ più padding sotto per dare respiro all’ultima card
                contentPadding = PaddingValues(bottom = 15.dp)
            ) {

                // ---------------- Header ----------------
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.user_menu_account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(id = R.string.user_menu_user_menu),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ---------------- Profile card ----------------
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.user_menu_profile),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            InfoFieldDark(
                                label = stringResource(R.string.user_menu_first_name),
                                valueRaw = profile?.firstName.orEmpty(),
                                allowMultiline = true
                            )
                            InfoFieldDark(
                                label = stringResource(R.string.user_menu_last_name),
                                valueRaw = profile?.lastName.orEmpty(),
                                allowMultiline = true
                            )
                            InfoFieldDark(
                                label = stringResource(R.string.user_menu_username),
                                valueRaw = profile?.username.orEmpty(),
                                allowMultiline = false
                            )
                            InfoFieldDark(
                                label = stringResource(R.string.user_menu_display_name),
                                valueRaw = profile?.displayName.orEmpty(),
                                allowMultiline = true
                            )
                            InfoFieldDark(
                                label = stringResource(R.string.user_menu_email),
                                valueRaw = email,
                                allowMultiline = true
                            )
                        }
                    }
                }

                // ---------------- Language section ----------------
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.user_menu_language),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = langLabel(currentLang),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text(
                                            stringResource(R.string.user_menu_language_label),
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PurpleAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                        focusedLabelColor = PurpleAccent,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                        cursorColor = PurpleAccent
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    languageOptions.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                expanded = false
                                                currentLang = code
                                                LocaleUtils.setLanguage(context, code)
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(id = R.string.user_menu_language_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                // ---------------- Footer button ----------------
                item {
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(id = R.string.action_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoFieldDark(
    label: String,
    valueRaw: String,
    allowMultiline: Boolean = true
) {
    val value = valueRaw.ifBlank { "—" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = if (allowMultiline) Int.MAX_VALUE else 1,
            overflow = if (allowMultiline) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}