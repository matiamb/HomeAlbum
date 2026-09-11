package com.example.homealbum.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.homealbum.R
import com.example.homealbum.model.MediaItem
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashScreen(
    trashViewModel: TrashViewModel,
    onBackFabClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    val trashUiState = trashViewModel.trashUiState.collectAsState()
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {result ->
        if (result.resultCode == Activity.RESULT_OK){
            trashViewModel.clearMultipleSelection()
            trashViewModel.loadTrashedFiles()
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Trash",
                        style = MaterialTheme.typography.displayMedium
                    )
                },
                navigationIcon = {
                    Image(
                        painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = ""
                    )
                }
            )
        },
        floatingActionButton = {
            TrashFabColumns(
                trashUiState = trashUiState.value,
                onBackFabClicked = onBackFabClicked,
                onRestoreFabClicked = {
                    trashViewModel.restoreFiles(onIntentReady = {intentSenderRequest ->
                        restoreLauncher.launch(intentSenderRequest)
                    })
                },
                onClearFabClicked = {
                    trashViewModel.clearMultipleSelection()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize()
        ) {
            TrashCardNotice()
            TrashGrid(
                trashUiState = trashUiState.value,
                onImageClicked = { uri ->
                    trashViewModel.enableMultipleSelection(uri)
                },
                onImageLongClick = { uri ->
                    trashViewModel.enableMultipleSelection(uri)
                },
                onRefresh = {
                    trashViewModel.loadTrashedFiles()
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashGrid(
    trashUiState: TrashUiState,
    onImageClicked: (Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
){
    PullToRefreshBox(
        isRefreshing = trashUiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = trashUiState.trashedFilesList){ item ->
                TrashImageThumbnail(
                    mediaItem = item,
                    trashUiState = trashUiState,
                    onImageClicked = onImageClicked,
                    onImageLongClick = onImageLongClick
                )
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashImageThumbnail(
    mediaItem: MediaItem,
    trashUiState: TrashUiState,
    onImageClicked: (Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
){
    val context = LocalContext.current
    val imageKey = "media-${mediaItem.uri}"
    val haptic = LocalHapticFeedback.current
    val selectedPadding by animateDpAsState(
        targetValue = if (trashUiState.multipleSelectionSet.isNotEmpty()
            && trashUiState.multipleSelectionSet.contains(mediaItem.uri)){
            8.dp
        } else {
            0.dp
        }
    )
    val thumbnail = if (mediaItem.isVideo){
        mediaItem.thumbnail
    } else{
        mediaItem.uri
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.animateContentSize()
    ){
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnail)
                .memoryCacheKey(imageKey)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .height(150.dp)
                //.clickable(true, onClick = { onImageClicked(index) }),
                .combinedClickable(
                    enabled = true,
                    onClick = { onImageClicked(mediaItem.uri) },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            hapticFeedbackType = HapticFeedbackType.LongPress
                        )
                        onImageLongClick(mediaItem.uri)
                    }
                ).padding(selectedPadding),
            contentScale = ContentScale.Crop
        )
        if (mediaItem.isVideo){
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = ""
            )
        }
        if (trashUiState.multipleSelectionSet.isNotEmpty()){
            if (trashUiState.multipleSelectionSet.contains(mediaItem.uri)){
                Icon(
                    painterResource(R.drawable.baseline_check_circle_24),
                    contentDescription = "",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TrashFabColumns(
    trashUiState: TrashUiState,
    onBackFabClicked: () -> Unit,
    onRestoreFabClicked: () -> Unit,
    onClearFabClicked: () -> Unit,
    modifier: Modifier = Modifier
){
    var isNavigating by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        if (trashUiState.multipleSelectionSet.isNotEmpty()){
            SmallFloatingActionButton(
                onClick = onRestoreFabClicked
            ) {
                Icon(
                    painterResource(R.drawable.rounded_undo_24),
                    contentDescription = "Restore"
                )
            }
            FloatingActionButton(
                onClick = onClearFabClicked
            ) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Clear"
                )
            }
        } else {
            FloatingActionButton(
                onClick = {
                    if(!isNavigating){
                        isNavigating = true
                        onBackFabClicked()
                    }
                }
            ) {
                Icon(
                    painterResource(R.drawable.baseline_home_filled_24),
                    contentDescription = "Back"
                )
            }
        }
    }
}
@Composable
fun TrashCardNotice(
    modifier: Modifier = Modifier
){
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = ""
            )
            Text(
                text = "Items in the trash will be deleted permanently after 30 days",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Composable
private fun TrashGridPreview(){
    val fakeItems = listOf(
        MediaItem(uri = "".toUri(), isVideo = false, dateTaken = 0L, null),
        MediaItem(uri = "".toUri(), isVideo = false, dateTaken = 0L, null),
        MediaItem(uri = "".toUri(), isVideo = false, dateTaken = 0L, null)
    )
    val trashUiState = TrashUiState(trashedFilesList = fakeItems)
    Surface(

    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TrashCardNotice()
            TrashGrid(
                trashUiState = trashUiState,
                onImageClicked = {uri -> },
                onImageLongClick = {uri -> },
                onRefresh = {},
            )
        }
    }
}