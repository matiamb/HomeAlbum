package com.example.homealbum.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTooltip(
    tooltipText: String,
    composable: @Composable () -> Unit,
    modifier: Modifier = Modifier
){
    val tooltipState = rememberTooltipState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible){
            haptic.performHapticFeedback(
                hapticFeedbackType = HapticFeedbackType.LongPress
            )
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip() {
                Text(
                    text = tooltipText
                )
            }
        },
        state = tooltipState,
        modifier = modifier
    ) {
        composable()
    }
}