package com.example.demo_musicsound.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.R
import com.example.demo_musicsound.auth.AuthViewModel
import com.example.mybeat.ui.theme.GrayBg
import com.example.mybeat.ui.theme.GraySurface
import com.example.mybeat.ui.theme.PurpleAccent
import com.example.mybeat.ui.theme.PurpleBar

private enum class AuthTab { LOGIN, REGISTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    vm: AuthViewModel,
    startOnRegister: Boolean = false,
    onDone: () -> Unit = {}
) {
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableStateOf(if (startOnRegister) AuthTab.REGISTER else AuthTab.LOGIN) }

    // Close screen on successful login/register
    LaunchedEffect(ui.isLoggedIn) {
        if (ui.isLoggedIn) onDone()
    }

    Scaffold(
        containerColor = GrayBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.auth_title_account),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PurpleBar
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraySurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    AuthTabs(
                        tab = tab,
                        onTab = {
                            tab = it
                            vm.clearMessage()
                        }
                    )

                    if (ui.loading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loadingBar")
                        )
                    }

                    ui.error?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text(it) },
                            modifier = Modifier.testTag("errorChip")
                        )
                    }

                    ui.message?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text(it) },
                            modifier = Modifier.testTag("messageChip")
                        )
                    }

                    // --------------------
                    // COMMON FIELDS
                    // --------------------

                    OutlinedTextField(
                        value = ui.email,
                        onValueChange = vm::setEmail,
                        label = { Text(stringResource(R.string.field_email)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emailField")
                    )

                    OutlinedTextField(
                        value = ui.password,
                        onValueChange = vm::setPassword,
                        label = { Text(stringResource(R.string.field_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passwordField")
                    )

                    // --------------------
                    // REGISTER-ONLY FIELDS
                    // --------------------

                    if (tab == AuthTab.REGISTER) {

                        OutlinedTextField(
                            value = ui.confirmPassword,
                            onValueChange = vm::setConfirmPassword,
                            label = { Text(stringResource(R.string.field_confirm_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirmField")
                        )

                        OutlinedTextField(
                            value = ui.firstName,
                            onValueChange = vm::setFirstName,
                            label = { Text(stringResource(R.string.field_first_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("firstNameField")
                        )

                        OutlinedTextField(
                            value = ui.lastName,
                            onValueChange = vm::setLastName,
                            label = { Text(stringResource(R.string.field_last_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lastNameField")
                        )

                        OutlinedTextField(
                            value = ui.username,
                            onValueChange = vm::setUsername,
                            label = { Text(stringResource(R.string.field_username)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("usernameField")
                        )

                    }

                    // --------------------
                    // SUBMIT
                    // --------------------

                    Button(
                        onClick = {
                            if (tab == AuthTab.LOGIN) vm.login() else vm.register()
                        },
                        enabled = !ui.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submitButton"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (tab == AuthTab.LOGIN)
                                stringResource(R.string.action_login)
                            else
                                stringResource(R.string.auth_action_create_account)
                        )
                    }

                    TextButton(
                        onClick = onDone,
                        enabled = !ui.loading,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("backButton")
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTabs(
    tab: AuthTab,
    onTab: (AuthTab) -> Unit
) {
    val pill = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PurpleBar, pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        @Composable
        fun Seg(
            text: String,
            selected: Boolean,
            tag: String,
            onClick: () -> Unit
        ) {
            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag(tag),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) PurpleAccent else Color.Transparent,
                    contentColor = if (selected) Color.Black else Color.White
                )
            ) {
                Text(text, fontWeight = FontWeight.SemiBold)
            }
        }

        Seg(
            text = stringResource(R.string.auth_tab_login),
            selected = tab == AuthTab.LOGIN,
            tag = "tabLogin"
        ) { onTab(AuthTab.LOGIN) }

        Seg(
            text = stringResource(R.string.auth_tab_register),
            selected = tab == AuthTab.REGISTER,
            tag = "tabRegister"
        ) { onTab(AuthTab.REGISTER) }
    }
}