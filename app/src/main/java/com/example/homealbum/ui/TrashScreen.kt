package com.example.homealbum.ui

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.homealbum.R
import com.example.homealbum.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashScreen(
    trashViewModel: TrashViewModel,
    onBackFabClicked: () -> Unit,
    //index: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
){
    val trashUiState = trashViewModel.trashUiState.collectAsState()
    var isNavigating by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
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
            FloatingActionButton(
                onClick = {
                    if(!isNavigating){
                        isNavigating = true
                        onBackFabClicked()
                    }
                }
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    ) { innerPadding ->
        TrashGrid(
            trashUiState = trashUiState.value,
            onImageClicked = {index, uri ->},
            onImageLongClick = {},
            //index = index,
            onRefresh = {},
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.padding(innerPadding)
        )
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashGrid(
    trashUiState: TrashUiState,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    //index: Int,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    //val galleryUiState = galleryViewModel.galleryUiState.collectAsState()

    with(sharedTransitionScope){
        PullToRefreshBox(
            isRefreshing = false,//galleryUiState.value.isRefreshing,
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
                       index = trashUiState.trashedFilesList.indexOf(item),
                       onImageClicked = onImageClicked,
                       onImageLongClick = onImageLongClick,
                       modifier = Modifier.sharedElement(
                           sharedContentState = rememberSharedContentState(
                               key = "media-$item"
                           ),
                           animatedVisibilityScope = animatedVisibilityScope
                       )
                   )
               }
            }
        }
    }

}
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TrashImageThumbnail(
    mediaItem: MediaItem,
    //galleryViewModel: GalleryViewModel,
    index: Int,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
){
    /**
     * Here the thumbnail value is initiated using produceState which makes this code run
     * inside a coroutine, so we can call the getThumbnail method from the viewModel
     */
//    val thumbnail by produceState<Bitmap?>(initialValue = null, mediaItem.uri) {
//        value = galleryViewModel.getThumbnail(mediaItem, 300, 300)
//    }
//    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()
    val context = LocalContext.current
    val imageKey = "media-${mediaItem.uri}"
    val haptic = LocalHapticFeedback.current
//    val selectedPadding by animateDpAsState(
//        targetValue = if (galleryUiState.value.multipleSelectionSet.isNotEmpty()
//            && galleryUiState.value.multipleSelectionSet.contains(mediaItem.uri)){
//            8.dp
//        } else {
//            0.dp
//        }
//    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.animateContentSize()
    ){
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .memoryCacheKey(imageKey)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .height(150.dp)
                //.clickable(true, onClick = { onImageClicked(index) }),
                .combinedClickable(
                    enabled = true,
                    onClick = { onImageClicked(index, mediaItem.uri) },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            hapticFeedbackType = HapticFeedbackType.LongPress
                        )
                        onImageLongClick(mediaItem.uri)
                    }
                )//.padding(selectedPadding),
            ,contentScale = ContentScale.Crop
        )
        if (mediaItem.isVideo){
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = ""
            )
        }
//        if (galleryUiState.value.multipleSelectionSet.isNotEmpty()){
//            if (galleryUiState.value.multipleSelectionSet.contains(mediaItem.uri)){
//                Icon(
//                    painterResource(R.drawable.baseline_check_circle_24),
//                    contentDescription = "",
//                    modifier = Modifier.align(Alignment.BottomEnd),
//                    tint = MaterialTheme.colorScheme.primary
//                )
//            }
//        }
    }
}
@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Composable
private fun TrashGridPreview(){
    val trashUiState = TrashUiState()
    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            TrashGrid(
                trashUiState = trashUiState,
                onImageClicked = { index, uri -> },
                onImageLongClick = { },
                //index = 0,
                onRefresh = {},
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this
            )
        }
    }
}