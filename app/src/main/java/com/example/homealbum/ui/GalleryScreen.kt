package com.example.homealbum.ui

import android.Manifest
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.homealbum.model.GalleryItem
import com.example.homealbum.model.MediaItem
import com.example.homealbum.model.ServerConnectionStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onSettingsFabClicked: () -> Unit,
    onImageClicked: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
){
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState() )
    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {GalleryTopBar(scrollBehavior, galleryUiState.value, onServerCheckClick = {galleryViewModel.checkServerConnection()})},
        floatingActionButton = {
            if (galleryUiState.value.multipleSelectionSet.isNotEmpty()){
                FloatingActionButton(
                    onClick = {
                        galleryViewModel.clearMultipleSelectionSet()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = ""
                    )
                }
            } else {
                SettingsFab(
                    onSettingsFabClicked,
                    sharedTransitionScope,
                    animatedVisibilityScope
                )
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
        }

        LaunchedEffect(hasPermission) {
            if (!hasPermission){
                permissionLauncher.launch(permissionToRequest)
            } else {
                galleryViewModel.loadPhotos()
            }
        }

        if(hasPermission){
            GalleryGrid(
                galleryViewModel = galleryViewModel,
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
    galleryViewModel: GalleryViewModel,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()

    with(sharedTransitionScope){
        PullToRefreshBox(
            isRefreshing = galleryUiState.value.isRefreshing,
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
                galleryUiState.value.galleryItems.forEach { item ->
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
                                    galleryViewModel = galleryViewModel,
                                    index = item.originalIndex,
                                    onImageClicked = onImageClicked,
                                    onImageLongClick = onImageLongClick,
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(
                                            key = "media-${item.originalIndex}"
                                        ),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }
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
){
    with(sharedTransitionScope){
        FloatingActionButton(
            onClick = onSettingsFabClicked,
            modifier = Modifier.sharedBounds(
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
    onServerCheckClick: () -> Unit
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
        },
        scrollBehavior = scrollBehavior
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
    galleryViewModel: GalleryViewModel,
    index: Int,
    onImageClicked: (Int, Uri) -> Unit,
    onImageLongClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
){
    /**
     * Here the thumbnail value is initiated using produceState which makes this code run
     * inside a coroutine, so we can call the getThumbnail method from the viewModel
     */
    val thumbnail by produceState<Bitmap?>(initialValue = null, mediaItem.uri) {
        value = galleryViewModel.getThumbnail(mediaItem, 300, 300)
    }
    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()
    val context = LocalContext.current
    val imageKey = "media-$index-${mediaItem.uri}"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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
                    onClick = { onImageClicked(index, mediaItem.uri) },
                    onLongClick = { onImageLongClick(mediaItem.uri) }
                ),
            contentScale = ContentScale.Crop
        )
        if (mediaItem.isVideo){
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = ""
            )
        }
        if (galleryUiState.value.multipleSelectionSet.isNotEmpty()){
            if (galleryUiState.value.multipleSelectionSet.contains(mediaItem.uri)){
                Icon(
                    Icons.Filled.Check,
                    contentDescription = ""
                )
            }
        }
    }
}

private fun openPermissionSettings(context: Context){
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Preview(showSystemUi = true)
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
