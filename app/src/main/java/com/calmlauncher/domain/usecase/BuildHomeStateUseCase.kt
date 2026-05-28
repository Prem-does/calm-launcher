package com.calmlauncher.domain.usecase

data class HomeState(
    val timeText: String,
    val dateText: String,
    val batteryText: String,
    val connectivityText: String,
    val weatherText: String? = null
)

class BuildHomeStateUseCase {
    fun invoke(
        timeText: String,
        dateText: String,
        batteryText: String,
        connectivityText: String,
        weatherText: String? = null
    ): HomeState {
        return HomeState(
            timeText = timeText,
            dateText = dateText,
            batteryText = batteryText,
            connectivityText = connectivityText,
            weatherText = weatherText
        )
    }
}
