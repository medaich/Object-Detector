package com.example.objectdetector.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ApiType(
    @StringRes
    val label: Int,
    @StringRes
    val description: Int,
    // drawable icon
    @DrawableRes
    val icon: Int
)
