package com.sahm.pos.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PrimaryTextField
import com.sahm.pos.screens.login.components.LoginHeader
import com.sahm.pos.theme.Black
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ErrorRed
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.theme.ShadowColor
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.ScreenType
import org.jetbrains.compose.resources.stringResource
import sahmfoodposapp.composeapp.generated.resources.Res
import sahmfoodposapp.composeapp.generated.resources.login_button
import sahmfoodposapp.composeapp.generated.resources.login_loading
import sahmfoodposapp.composeapp.generated.resources.login_password_hide
import sahmfoodposapp.composeapp.generated.resources.login_password_label
import sahmfoodposapp.composeapp.generated.resources.login_password_placeholder
import sahmfoodposapp.composeapp.generated.resources.login_password_show
import sahmfoodposapp.composeapp.generated.resources.login_phone_label
import sahmfoodposapp.composeapp.generated.resources.login_phone_placeholder
import sahmfoodposapp.composeapp.generated.resources.login_subtitle
import sahmfoodposapp.composeapp.generated.resources.login_title

@Composable
fun LoginScreen(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .safeContentPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        when (screenType) {
            ScreenType.Phone -> {
                PhoneLoginContent(
                    state = state,
                    screenType = screenType,
                    onIntent = onIntent,
                )
            }

            ScreenType.Tablet -> {
                TabletLoginContent(
                    state = state,
                    screenType = screenType,
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun PhoneLoginContent(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterVertically)
    ) {
        LoginHeader(screenType)

        LoginCard(
            state = state,
            screenType = screenType,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun TabletLoginContent(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.Center,
        ) {
            LoginHeader(
                screenType = screenType
            )
        }

        Box(
            modifier = Modifier.weight(0.6f),
            contentAlignment = Alignment.Center,
        ) {
            LoginCard(
                state = state,
                screenType = screenType,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun LoginCard(
    state: LoginUiState,
    screenType: ScreenType,
    onIntent: (LoginIntent) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ShadowColor,
                spotColor = ShadowColor,
            ),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            PrimaryTextField(
                label = stringResource(Res.string.login_phone_label),
                value = state.phone,
                placeholder = stringResource(Res.string.login_phone_placeholder),
                error = state.phoneError?.let { stringResource(it) },
                onValueChange = { onIntent(LoginIntent.PhoneChanged(it)) },
                screenType = screenType,
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(24.dp))

            PrimaryTextField(
                label = stringResource(Res.string.login_password_label),
                value = state.password,
                placeholder = stringResource(Res.string.login_password_placeholder),
                error = state.passwordError?.let { stringResource(it) },
                keyboardType = KeyboardType.NumberPassword,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingContent = {
                    TextButton(
                        onClick = { onIntent(LoginIntent.TogglePasswordVisibility) },
                    ) {
                        Text(
                            text = if (state.isPasswordVisible) {
                                stringResource(Res.string.login_password_hide)
                            } else {
                                stringResource(Res.string.login_password_show)
                            },
                        )
                    }
                },
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                screenType = screenType
            )

            state.generalError?.let { generalError ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(generalError),
                    color = ErrorRed,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onIntent(LoginIntent.SubmitLogin) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    disabledContainerColor = PrimaryOrange.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 15.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.login_loading),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.login_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}


private data class LoginTypography(
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    val labelSize: TextUnit,
    val inputSize: TextUnit,
    val buttonTextSize: TextUnit,
    val checkboxSize: TextUnit,
    val errorSize: TextUnit,
) {
    companion object {
        val Phone = LoginTypography(
            titleSize = 34.sp,
            subtitleSize = 18.sp,
            labelSize = 16.sp,
            inputSize = 17.sp,
            buttonTextSize = 18.sp,
            checkboxSize = 15.sp,
            errorSize = 13.sp,
        )

        val Tablet = LoginTypography(
            titleSize = 48.sp,
            subtitleSize = 24.sp,
            labelSize = 18.sp,
            inputSize = 20.sp,
            buttonTextSize = 22.sp,
            checkboxSize = 18.sp,
            errorSize = 14.sp,
        )
    }
}
