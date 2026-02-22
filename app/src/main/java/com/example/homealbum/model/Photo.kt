package com.example.homealbum.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Photo (
    @DrawableRes val photoRes: Int,
    @StringRes val descriptionRes: Int
)