package com.sahm.pos.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahm.pos.theme.Black
import com.sahm.pos.theme.ErrorRed
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.ScreenType

@Composable
fun PrimaryTextField(
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    screenType: ScreenType,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Text(
        text = label,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = TextSecondary,
                fontSize = 16.sp,
            )
        },
        textStyle = LocalTextStyle.current.copy(
            fontSize = 16.sp,
            color = TextPrimary,
        ),
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Black),
        visualTransformation = visualTransformation,
        trailingIcon = trailingContent,
        shape = RoundedCornerShape(14.dp),
        supportingText = error?.let {
            {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 12.sp,
                )
            }
        },
    )
}