package com.example.homealbum.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackFabPressed: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
){
    var isNavigating by remember { mutableStateOf(false) }
    with(sharedTransitionScope){
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (!isNavigating){
                            isNavigating = true
                            onBackFabPressed()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        ) {innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "settings-screen"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
                SettingItemCard(
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

        }
    }

}
@Composable
fun SettingItemCard(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState = settingsViewModel.uiState.collectAsState()
    var textIp by remember(settingsState.value.serverIp) { mutableStateOf(settingsState.value.serverIp) }
    var textFolderName by remember(settingsState.value.serverFolderName) { mutableStateOf(settingsState.value.serverFolderName) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
//    var isEnabled by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        LaunchedEffect(Unit) {
            settingsViewModel.toastMessage.collect { message ->
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = ""
            )
            Text(
                text = "Settings",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.displayLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Enable local server backup?"
                )
                Switch(
                    checked = settingsState.value.isBackupEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(
                            hapticFeedbackType = HapticFeedbackType.ToggleOn
                        )
                        settingsViewModel.saveBackupEnabled(it)
                                      },
                    thumbContent = {
                        if (settingsState.value.isBackupEnabled){
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = ""
                            )
                        }
                    }
                )
            }
            TextField(
                value = textIp,
                onValueChange = { newText -> textIp = newText},
                label = {
                    Text(
                        text = "Please enter the server ip"
                    )
                },
                placeholder = {
                    Text(
                        text = "Example: 100.0.0.1"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = settingsState.value.isBackupEnabled,
                maxLines = 1
            )
            TextField(
                value = textFolderName,
                onValueChange = { newText -> textFolderName = newText},
                label = {
                    Text(
                        text = "Please enter the folder name"
                    )
                },
                placeholder = {
                    Text(
                        text = "Example: Folder/subFolder"
                    )
                },
                enabled = settingsState.value.isBackupEnabled,
                maxLines = 1
            )
            Button(
                onClick = {
                    settingsViewModel.saveServerSettings(textIp, textFolderName)
//                    Toast.makeText(
//                        context,
//                        "Settings saved!",
//                        Toast.LENGTH_LONG
//                    ).show()
                },
                enabled = settingsState.value.isBackupEnabled && !settingsViewModel.isChecking.value
            ) {
                if (settingsViewModel.isChecking.value){
                    Text(
                        text = "Checking..."
                    )
                } else {
                    Text(
                        text = "Save"
                    )
                }
            }
        }
    }
}

//@Preview(showSystemUi = true)
//@Composable
//fun SettingItemCardPreview(){
//    SettingsScreen(
//        settingsViewModel = ,
//        onBackFabPressed = {}
//    )
//}