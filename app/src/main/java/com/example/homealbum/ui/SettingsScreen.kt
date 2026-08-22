package com.example.homealbum.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homealbum.R
import com.example.homealbum.data.SettingsRepository
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.model.UserSettings

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackFabPressed: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
){
    var isNavigating by remember { mutableStateOf(false) }
    val settingsState = settingsViewModel.userSettings.collectAsState()
    val settingsUiState = settingsViewModel.settingsUiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        settingsViewModel.toastMessage.collect { message ->
            Toast.makeText(
                context,
                message.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
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
                    settingsState = settingsState.value,
                    settingsUiState = settingsUiState.value,
                    onBackupSwitched = { value ->
                        settingsViewModel.saveBackupEnabled(value)
                    },
                    onMobileDataSwitched = { value ->
                        settingsViewModel.saveMobileDataUpload(value)
                    },
                    onSaveClicked = { textIp, textFolderName ->
                        settingsViewModel.saveServerSettings(textIp, textFolderName)
                    },
                    //isChecking = settingsViewModel.isChecking.value,
                    modifier = Modifier.padding(innerPadding)
                )
            }

        }
    }

}
@Composable
fun SettingItemCard(
    settingsState: UserSettings,
    settingsUiState: SettingsUiState,
    onBackupSwitched: (Boolean) -> Unit,
    onMobileDataSwitched: (Boolean) -> Unit,
    onSaveClicked: (String, String) -> Unit,
    //isChecking: Boolean,
    modifier: Modifier = Modifier
) {
    //val settingsState = settingsViewModel.userSettings.collectAsState()
    var textIp by remember(settingsState.serverIp) { mutableStateOf(settingsState.serverIp) }
    var textFolderName by remember(settingsState.serverFolderName) { mutableStateOf(settingsState.serverFolderName) }
    //val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
//        LaunchedEffect(Unit) {
//            settingsViewModel.toastMessage.collect { message ->
//                Toast.makeText(
//                    context,
//                    message.message,
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = ""
            )
            Text(
                text = stringResource(R.string.settings),
                //modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.displayLarge
            )
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceAround
//            ) {
                OptionSwitch(
                    optionText = R.string.enable_local_server_backup,
                    checked = settingsState.isBackupEnabled,
                    onCheckedChange = {value ->
                        haptic.performHapticFeedback(
                            hapticFeedbackType = HapticFeedbackType.ToggleOn
                        )
                        onBackupSwitched(value)
                        //settingsViewModel.saveBackupEnabled(value)
                    }
                )
                OptionSwitch(
                    optionText = R.string.allow_upload_using_mobile_data,
                    checked = settingsState.allowUploadMobileData,
                    onCheckedChange = {value ->
                        haptic.performHapticFeedback(
                            hapticFeedbackType = HapticFeedbackType.ToggleOn
                        )
                        //settingsViewModel.saveMobileDataUpload(value)
                        onMobileDataSwitched(value)
                    }
                )
//                Text(
//                    text = stringResource(R.string.enable_local_server_backup)
//                )
//                Switch(
//                    checked = settingsState.value.isBackupEnabled,
//                    onCheckedChange = {
//                        haptic.performHapticFeedback(
//                            hapticFeedbackType = HapticFeedbackType.ToggleOn
//                        )
//                        settingsViewModel.saveBackupEnabled(it)
//                                      },
//                    thumbContent = {
//                        if (settingsState.value.isBackupEnabled){
//                            Icon(
//                                Icons.Filled.Check,
//                                contentDescription = ""
//                            )
//                        }
//                    }
//                )
            //}
            TextField(
                value = textIp,
                onValueChange = { newText -> textIp = newText},
                label = {
                    Text(
                        text = stringResource(R.string.please_enter_the_server_ip)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.example_ip_msg)
                    )
                },
                trailingIcon = {
                    when(settingsUiState.serverConnectionStatus){
                        ServerConnectionStatus.CHECKING ->{
                            CircularProgressIndicator()
                        }
                        ServerConnectionStatus.CONNECTED -> {
                            Icon(
                                painter = painterResource(R.drawable.outline_computer_24),
                                contentDescription = "",
                                tint = Color(0xff2eef68)
                            )
                        }
                        ServerConnectionStatus.FAILED -> {
                            Icon(
                                painter = painterResource(R.drawable.outline_mimo_disconnect_24),
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                enabled = settingsState.isBackupEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = textFolderName,
                onValueChange = { newText -> textFolderName = newText},
                label = {
                    Text(
                        text = stringResource(R.string.please_enter_the_folder_name)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.example_folder_msg)
                    )
                },
                enabled = settingsState.isBackupEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onSaveClicked(textIp, textFolderName)
                    //settingsViewModel.saveServerSettings(textIp, textFolderName)
                },
                enabled = settingsState.isBackupEnabled && !settingsUiState.isChecking//!settingsViewModel.isChecking.value
            ) {
                if (settingsUiState.isChecking){
                    Text(
                        text = stringResource(R.string.checking_btn)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_btn)
                    )
                }
            }
        }
    }
}
@Composable
fun OptionSwitch(
    optionText: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
{
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(optionText)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
//                {
//                haptic.performHapticFeedback(
//                    hapticFeedbackType = HapticFeedbackType.ToggleOn
//                )
//                onSwitchChange()
//                //settingsViewModel.saveBackupEnabled(it)
//            },
            thumbContent = {
                if (checked) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = ""
                    )
                }
            }
        )
    }
}
@Preview
@Composable
private fun SettingsScreenPreview(){
    val settingsState = UserSettings("","", false, false)
    val settingsUiState = SettingsUiState()
    SettingItemCard(
        settingsState = settingsState,
        settingsUiState = settingsUiState,
        onBackupSwitched = {},
        onMobileDataSwitched = {},
        onSaveClicked = {textIp, folderName ->},
        //isChecking = false,
        )
}
