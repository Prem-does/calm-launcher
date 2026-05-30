package com.calmlauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmError
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * PIN Protection. When a PIN is set: a field for the current PIN plus a "Remove PIN" button that
 * verifies before clearing. When no PIN is set: a field for a new PIN plus a "Set PIN" button.
 * The numeric entry is masked but kept intentionally simple — this is friction, not security.
 */
@Composable
fun PinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = hiltViewModel(),
) {
    val pinEnabled by viewModel.pinEnabled.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "PIN Protection", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(Spacing.marginMobile),
        ) {
            Text(
                text = if (pinEnabled) "Enter your PIN to remove it." else "Set a numeric PIN.",
                style = CalmType.bodyLg,
                color = CalmGray,
                modifier = Modifier.padding(top = Spacing.stackLg),
            )

            PinField(
                value = input,
                onValueChange = {
                    input = it.filter(Char::isDigit)
                    error = false
                },
                placeholder = if (pinEnabled) "Current PIN" else "New PIN",
            )

            if (error) {
                Text(text = "Incorrect PIN.", style = CalmType.bodyMd, color = CalmError)
            }

            if (pinEnabled) {
                CalmButton(
                    text = "Remove PIN",
                    style = CalmButtonStyle.Outlined,
                    enabled = input.isNotBlank(),
                    onClick = {
                        viewModel.removePin(input) { ok ->
                            if (ok) input = "" else error = true
                        }
                    },
                )
            } else {
                CalmButton(
                    text = "Set PIN",
                    style = CalmButtonStyle.Filled,
                    enabled = input.isNotBlank(),
                    onClick = {
                        viewModel.setPin(input)
                        input = ""
                    },
                )
            }
        }
    }
}

/**
 * A minimal monochrome numeric field: masked digits in [CalmType.headlineMd], a [CalmGray]
 * placeholder, and a closing [ThinDivider] underline. No Material chrome — matches the launcher's
 * Swiss surfaces.
 */
@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = CalmType.headlineMd.copy(color = CalmWhite),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(CalmWhite),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.stackMd),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = CalmType.headlineMd, color = CalmGray)
                    }
                    inner()
                }
            },
        )
        ThinDivider()
    }
}
