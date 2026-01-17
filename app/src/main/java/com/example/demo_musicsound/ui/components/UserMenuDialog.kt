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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.demo_musicsound.R
import com.example.demo_musicsound.community.UserProfile
import com.example.demo_musicsound.ui.util.LocaleUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMenuDialog(
    profile: UserProfile?,
    emailFallback: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // lingua corrente (es: "en", "it", "fr")
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
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // ---------------- Header ----------------
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.user_menu_account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(id = R.string.user_menu_user_menu),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                }

                // ---------------- Profile card ----------------
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.user_menu_profile),
                                fontWeight = FontWeight.SemiBold
                            )

                            InfoRowNice(stringResource(R.string.user_menu_display_name), profile?.displayName.orEmpty())
                            InfoRowNice(stringResource(R.string.user_menu_username), profile?.username.orEmpty())
                            InfoRowNice(stringResource(R.string.user_menu_first_name), profile?.firstName.orEmpty())
                            InfoRowNice(stringResource(R.string.user_menu_last_name), profile?.lastName.orEmpty())
                            InfoRowNice(stringResource(R.string.user_menu_email), email)
                        }
                    }
                }

                // ---------------- Language section ----------------
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.user_menu_language),
                                fontWeight = FontWeight.SemiBold
                            )

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = langLabel(currentLang),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.user_menu_language_label)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    }
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRowNice(label: String, valueRaw: String) {
    val value = valueRaw.ifBlank { "—" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
