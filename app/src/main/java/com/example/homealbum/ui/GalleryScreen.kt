package com.example.homealbum.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.homealbum.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.homealbum.model.GalleryItem
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.ServerConnectionStatus
import com.example.homealbum.ui.components.BaseTooltip
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onSettingsFabClicked: () -> Unit,
    onImageClicked: (Int) -> Unit,
    onSmallFabClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState() )
    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()
    val context = LocalContext.current
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {result ->
        if (result.resultCode == Activity.RESULT_OK){
            galleryViewModel.removeThrashedPhotoFromUi(galleryUiState.value.multipleSelectionSet)
            galleryViewModel.removeMediaFromServer(galleryUiState.value.multipleSelectionSet)
            galleryViewModel.loadPhotos()
            galleryViewModel.clearMultipleSelectionSet()
        }
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GalleryTopBar(
                scrollBehavior,
                galleryUiState.value,
                onServerCheckClick = {galleryViewModel.checkServerConnection()}
            )
                 },
        floatingActionButton = {
            if (galleryUiState.value.multipleSelectionSet.isNotEmpty()){
                FabButtonsColumn(
                    onDeleteClicked = {
                        galleryViewModel.requestTrashPhoto(){ intentSenderRequest ->
                            deleteLauncher.launch(intentSenderRequest)
                        }
                    },
                    onUploadClicked = {
                        galleryViewModel.startMultipleUpload()
                    },
                    onShareClicked = {
                        sharePhoto(context, galleryUiState.value.multipleSelectionSet)
                        galleryViewModel.clearMultipleSelectionSet()
                    },
                    onClearSelectionClicked = {
                        galleryViewModel.clearMultipleSelectionSet()
                    }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
                        with(sharedTransitionScope){
                            BaseTooltip(
                                tooltipText = "Open trash can",
                                composable = {
                                    SmallFloatingActionButton(
                                        onClick = onSmallFabClicked,
                                        modifier = Modifier.padding(bottom = 4.dp).sharedBounds(
                                            sharedContentState = rememberSharedContentState(
                                                key = "trash-screen"
                                            ),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.outline_recycling_24),
                                            contentDescription = "Trash can"
                                        )
                                    }
                                }
                            )
                        }
                    }
                    SettingsFab(
                        onSettingsFabClicked,
                        sharedTransitionScope,
                        animatedVisibilityScope
                    )
                }

            }
        },
    ) { innerPadding ->
        val context = LocalContext.current

        val permissionToRequest = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        var hasPermission by remember {
            mutableStateOf(
                permissionToRequest.all { permission ->
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                }
            )
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ){ permissionMap ->
            hasPermission = permissionMap.values.all { isGranted -> isGranted }
            galleryViewModel.loadPhotos()
        }

        LaunchedEffect(hasPermission) {
            if (!hasPermission){
                permissionLauncher.launch(permissionToRequest)
            }
        }

        if(hasPermission){
            GalleryGrid(
                galleryUiState = galleryUiState.value,
                onImageClicked = { index, uri ->
                    if (galleryUiState.value.multipleSelectionSet.isEmpty()){
                        onImageClicked(index)
                    } else {
                        galleryViewModel.multipleSelection(uri)
                    }
                                 },
                onImageLongClick = { uri ->
                    galleryViewModel.multipleSelection(uri)
                },
                onRefresh = {galleryViewModel.loadPhotos()},
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                RequestPermissionFab(
                    onRequestPermissionClicked = {
                        openPermissionSettings(context)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun GalleryGrid(
    galleryUiState: GalleryUiState,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    PullToRefreshBox(
        isRefreshing = galleryUiState.isRefreshing,
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
            galleryUiState.galleryItems.forEach { item ->
                when(item){
                    is GalleryItem.DateHeader -> {
                        item(key = "header-${item.date}",
                            span = { GridItemSpan(maxLineSpan)
                            }
                        ){
                            Text(
                                text = item.date.format(
                                    DateTimeFormatter.ofPattern("dd MMMM yyyy")
                                ),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    is GalleryItem.Photo -> {
                        item(key = item.mediaItem.uri){
                            ImageThumbnail(
                                mediaItem = item.mediaItem,
                                galleryUiState = galleryUiState,
                                index = item.originalIndex,
                                onImageClicked = onImageClicked,
                                onImageLongClick = onImageLongClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsFab(
    onSettingsFabClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    with(sharedTransitionScope){
        FloatingActionButton(
            onClick = onSettingsFabClicked,
            modifier = modifier.padding(top = 4.dp).sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "settings-screen"
                ),
                animatedVisibilityScope = animatedVisibilityScope
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = ""
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GalleryTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    uiState: GalleryUiState,
    onServerCheckClick: () -> Unit,
    modifier: Modifier = Modifier
){
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
        },
        actions = {
            if (uiState.multipleSelectionSet.isNotEmpty()){
                Text(
                    text = uiState.multipleSelectionSet.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                BaseTooltip(
                    tooltipText = "Check server connection",
                    composable = {
                        IconButton(
                            onClick = onServerCheckClick
                        ) {
                            when(uiState.serverConnectionStatus){
                                ServerConnectionStatus.CHECKING -> {
                                    CircularProgressIndicator()
                                }
                                ServerConnectionStatus.CONNECTED -> {
                                    Icon(
                                        painterResource(R.drawable.outline_computer_24),
                                        contentDescription = "",
                                        tint = Color(0xff2eef68)
                                    )
                                }
                                ServerConnectionStatus.FAILED -> {
                                    Icon(
                                        painterResource(R.drawable.outline_mimo_disconnect_24),
                                        contentDescription = "",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
fun RequestPermissionFab(
    onRequestPermissionClicked: () -> Unit,
    modifier: Modifier = Modifier
){
    FloatingActionButton(
        onRequestPermissionClicked,
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = stringResource(R.string.request_permission_fab),
            modifier = Modifier.padding(8.dp)
        )
    }
}
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ImageThumbnail(
    mediaItem: MediaItem,
    galleryUiState: GalleryUiState,
    index: Int,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    val thumbnail = if (mediaItem.isVideo){
        mediaItem.thumbnail
    } else {
        mediaItem.uri
    }
    val context = LocalContext.current
    val imageKey = "media-$index-${mediaItem.uri}"
    val haptic = LocalHapticFeedback.current
    val selectedPadding by animateDpAsState(
        targetValue = if (galleryUiState.multipleSelectionSet.isNotEmpty()
            && galleryUiState.multipleSelectionSet.contains(mediaItem.uri)){
            8.dp
        } else {
            0.dp
        }
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.animateContentSize()
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnail)
                    .size(300, 300)
                    .memoryCacheKey(imageKey)
                    .build(),
                contentDescription = "",
                modifier = Modifier
                    .height(150.dp)
                    .combinedClickable(
                        enabled = true,
                        onClick = { onImageClicked(index, mediaItem.uri) },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                hapticFeedbackType = HapticFeedbackType.LongPress
                            )
                            onImageLongClick(mediaItem.uri)
                        }
                    ).padding(selectedPadding)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = "media-$index"
                        ),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                contentScale = ContentScale.Crop
            )
            if (mediaItem.isVideo) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = ""
                )
            }
            if (galleryUiState.multipleSelectionSet.isNotEmpty()) {
                if (galleryUiState.multipleSelectionSet.contains(mediaItem.uri)) {
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
}
@Composable
fun FabButtonsColumn(
    onDeleteClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onClearSelectionClicked: () -> Unit,
    modifier: Modifier = Modifier
){
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        SmallFloatingActionButton(
            onClick = onDeleteClicked
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete"
            )
        }
        SmallFloatingActionButton(
            onClick = onShareClicked
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = "Share"
            )
        }
        BaseTooltip(
            tooltipText = "Upload to server",
            composable = {
                SmallFloatingActionButton(
                    onClick = onUploadClicked
                ) {
                    Icon(
                        painterResource(R.drawable.outline_cloud_upload_24),
                        contentDescription = "Upload"
                    )
                }
            }
        )

        FloatingActionButton(
            onClick = onClearSelectionClicked,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear selection"
            )
        }
    }
}

private fun openPermissionSettings(context: Context){
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
private fun sharePhoto(context: Context, uriSet: Set<Uri>){
    val uriArray = ArrayList(uriSet)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putExtra(Intent.EXTRA_STREAM, uriArray)
        type = "*/*"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    val appChooser = Intent.createChooser(shareIntent, "Share on...")
    context.startActivity(appChooser)
}

@Preview(showSystemUi = false)
@Composable
private fun RequestPermissionPreview(){
    RequestPermissionFab(
        onRequestPermissionClicked = {}
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GalleryTopBarPreview(){
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState() )
    GalleryTopBar(
        scrollBehavior = scrollBehavior,
        uiState = GalleryUiState(serverConnectionStatus = ServerConnectionStatus.CONNECTED),
        onServerCheckClick = {}
    )
}
