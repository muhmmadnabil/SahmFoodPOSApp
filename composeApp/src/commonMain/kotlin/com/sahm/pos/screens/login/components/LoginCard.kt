package com.sahm.pos.screens.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.components.PrimaryTextField
import com.sahm.pos.screens.login.LoginIntent
import com.sahm.pos.screens.login.LoginUiState
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.ErrorRed
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.ShadowColor
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

@Composable
fun LoginCard(
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