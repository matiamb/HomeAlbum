package com.example.homealbum.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTooltip(
    tooltipText: String,
    composable: @Composable () -> Unit,
    modifier: Modifier = Modifier
){
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip() {
                Text(
                    text = tooltipText
                )
            }
        },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        composable()
    }
}