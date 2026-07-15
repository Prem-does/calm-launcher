package com.calmlauncher.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.domain.model.LauncherSettings

/**
 * A compact list of advanced toggles moved out of the primary Settings surface.
 */
@Composable
fun AdvancedSettings(viewModel: SettingsViewModel, settings: LauncherSettings) {
    Column {
        SettingRow(
            title = "Dopamine Detection",
            trailing = {
                CalmToggle(settings.dopamineDetectionEnabled) { checked ->
                    viewModel.update { it.copy(dopamineDetectionEnabled = checked) }
                }
            },
        )
        SettingRow(
            title = "Dynamic Minimalism",
            trailing = {
                CalmToggle(settings.dynamicMinimalismEnabled) { checked ->
                    viewModel.update { it.copy(dynamicMinimalismEnabled = checked) }
                }
            },
        )
        SettingRow(
            title = "Reward Real Life",
            trailing = {
                CalmToggle(settings.rewardRealLifeEnabled) { checked ->
                    viewModel.update { it.copy(rewardRealLifeEnabled = checked) }
                }
            },
        )
        SettingRow(
            title = "Recovery Mode",
            trailing = {
                CalmToggle(settings.recoveryModeEnabled) { checked ->
                    viewModel.update { it.copy(recoveryModeEnabled = checked) }
                }
            },
        )
        SettingRow(
            title = "Dead-End Feeds",
            trailing = {
                CalmToggle(settings.deadEndFeedsEnabled) { checked ->
                    viewModel.update { it.copy(deadEndFeedsEnabled = checked) }
                }
            },
        )

        Spacer(Modifier.height(12.dp))
    }
}
