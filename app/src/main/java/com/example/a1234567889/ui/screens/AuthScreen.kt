package com.example.a1234567889.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.a1234567889.R
import com.example.a1234567889.models.ForgotPasswordResult

// ─────────────────────────────────────────────────────────────
// Validation helpers
// ─────────────────────────────────────────────────────────────

private fun isValidEmail(email: String): Boolean =
    email.contains('@') && email.contains('.') && email.indexOf('@') < email.lastIndexOf('.')

private fun isValidPassword(password: String): Boolean = password.length >= 6

private fun isValidNickname(nickname: String): Boolean = nickname.length >= 2

// ─────────────────────────────────────────────────────────────
// Auth screen
// ─────────────────────────────────────────────────────────────

@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGuest: () -> Unit,
    onForgotPassword: ((String) -> ForgotPasswordResult)? = null,
    errorMessage: String? = null
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var emailTouched by remember { mutableStateOf(false) }
    var nicknameTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }

    val emailError = if (emailTouched && email.isNotEmpty() && !isValidEmail(email))
        "Введите корректный email (должен содержать @)" else null
    val passwordError = if (passwordTouched && password.isNotEmpty() && !isValidPassword(password))
        "Пароль должен содержать не менее 6 символов" else null
    val nicknameError = if (!isLoginMode && nicknameTouched && nickname.isNotEmpty() && !isValidNickname(nickname))
        "Никнейм должен содержать не менее 2 символов" else null

    val canSubmit = if (isLoginMode) {
        email.isNotBlank() && password.isNotBlank() && isValidEmail(email) && isValidPassword(password)
    } else {
        email.isNotBlank() && password.isNotBlank() && nickname.isNotBlank() &&
                isValidEmail(email) && isValidPassword(password) && isValidNickname(nickname)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Benefits Section
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Зачем нужен аккаунт?", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                BenefitRow(Icons.Default.CloudSync, "Сохранение прогресса на всех устройствах")
                BenefitRow(Icons.Default.EmojiEvents, "Участие в мировых рейтингах и турнирах")
                BenefitRow(Icons.Default.People, "Игра с друзьями и обмен рекордами")
            }
        }

        Text(
            text = if (isLoginMode) stringResource(R.string.login) else stringResource(R.string.register),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailTouched = true },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            isError = emailError != null,
            supportingText = if (emailError != null) {
                { Text(emailError, color = MaterialTheme.colorScheme.error) }
            } else null,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Nickname (register only)
        if (!isLoginMode) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it; nicknameTouched = true },
                label = { Text(stringResource(R.string.nickname)) },
                modifier = Modifier.fillMaxWidth(),
                isError = nicknameError != null,
                supportingText = if (nicknameError != null) {
                    { Text(nicknameError, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordTouched = true },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError != null,
            supportingText = if (passwordError != null) {
                { Text(passwordError, color = MaterialTheme.colorScheme.error) }
            } else {
                { Text(if (isLoginMode) "" else "Минимум 6 символов", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль"
                    )
                }
            }
        )

        // Server/auth error
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = {
                emailTouched = true
                passwordTouched = true
                nicknameTouched = true
                if (canSubmit) {
                    if (isLoginMode) onLogin(email.trim(), password)
                    else onRegister(email.trim(), nickname.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoginMode) stringResource(R.string.login) else stringResource(R.string.register),
                maxLines = 1,
                softWrap = false
            )
        }

        // Forgot password (only in login mode)
        if (isLoginMode) {
            TextButton(onClick = { showForgotDialog = true }) {
                Text("Забыли пароль?", maxLines = 1, softWrap = false)
            }
        }

        TextButton(onClick = {
            isLoginMode = !isLoginMode
            emailTouched = false
            nicknameTouched = false
            passwordTouched = false
            showPassword = false
        }) {
            Text(
                text = if (isLoginMode) stringResource(R.string.no_account) else stringResource(R.string.have_account),
                maxLines = 1,
                softWrap = false
            )
        }

        TextButton(
            onClick = onGuest,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Default.NoAccounts, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.guest), maxLines = 1, softWrap = false)
        }
    }

    // Forgot password dialog
    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onSend = { enteredEmail ->
                val result = onForgotPassword?.invoke(enteredEmail) ?: ForgotPasswordResult.EmailSent
                result
            },
            onDismiss = { showForgotDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Forgot Password Dialog
// ─────────────────────────────────────────────────────────────

@Composable
fun ForgotPasswordDialog(
    initialEmail: String = "",
    onSend: (email: String) -> ForgotPasswordResult,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var touched by remember { mutableStateOf(false) }
    var resultState by remember { mutableStateOf<ForgotPasswordResult?>(null) }

    val emailError = if (touched && !isValidEmail(email.trim()))
        "Введите корректный email" else null

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {

                if (resultState == ForgotPasswordResult.EmailSent) {
                    // Success state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Письмо отправлено",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Если аккаунт с таким email существует, вы получите письмо с инструкцией по сбросу пароля.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Понятно", maxLines = 1, softWrap = false) }
                    }
                } else {
                    // Input state
                    Text("Восстановление пароля", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Введите email, привязанный к вашему аккаунту. Мы пришлём инструкцию по сбросу пароля.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; touched = true },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, null) }
                    )

                    if (resultState == ForgotPasswordResult.InvalidEmail) {
                        Text(
                            "Введите корректный email",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Отмена", maxLines = 1, softWrap = false) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                touched = true
                                if (isValidEmail(email.trim())) {
                                    resultState = onSend(email.trim())
                                } else {
                                    resultState = ForgotPasswordResult.InvalidEmail
                                }
                            }
                        ) { Text("Отправить", maxLines = 1, softWrap = false) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun AuthScreenPreview() {
    AuthScreen(onLogin = { _, _ -> }, onRegister = { _, _, _ -> }, onGuest = {})
}
