package com.example.homealbum.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.homealbum.R
import kotlinx.coroutines.launch

@Composable
fun ImageScreen(
    galleryViewModel: GalleryViewModel,
    initialPageIndex: Int,
    onBackFabClicked: () -> Unit,
    onLastPhotoDeleted: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    val uiState = galleryViewModel.galleryUiState.collectAsState()
    val openAlertDialog = remember{mutableStateOf(false)}
    val safeInitialPage = minOf(initialPageIndex, uiState.value.photoList.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = {uiState.value.photoList.size}
    )
    val context = LocalContext.current
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {result ->
        if (result.resultCode == Activity.RESULT_OK){
            val currentUri = uiState.value.photoList[pagerState.currentPage].uri
            val photoCount = uiState.value.photoList.size
            if (photoCount == 1){
                galleryViewModel.removeThrashedPhotoFromUi(currentUri)
                onLastPhotoDeleted()
            } else {
                galleryViewModel.removeThrashedPhotoFromUi(currentUri)
            }
        }
    }
    LaunchedEffect(Unit) {
        galleryViewModel.toastMessage.collect { message ->
            Log.d("Mati", "Toast message is: $message")
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    Scaffold(
        bottomBar = { BottomToolbar(
            onDeleteClicked = {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){
                    openAlertDialog.value = true
                }else{
                    val currentUri = uiState.value.photoList[pagerState.currentPage].uri
                    galleryViewModel.requestTrashPhoto(currentUri){ intentSenderRequest ->
                        deleteLauncher.launch(intentSenderRequest)
                    }
                }
                              },
            onSharedClicked = {
                val currentUri = uiState.value.photoList[pagerState.currentPage].uri
                sharePhoto(context =context, uri = currentUri )
                              },
            onBackFabClicked = onBackFabClicked,
            checkPhotoIsUploaded = {
                val currentUri = uiState.value.photoList[pagerState.currentPage].uri
                galleryViewModel.checkIfPhotoExists(currentUri)
            },
            onUploadClicked = {
                val currentUri = uiState.value.photoList[pagerState.currentPage].uri
                galleryViewModel.uploadPhoto(currentUri)
            }
        ) },
        modifier = modifier
    ) { paddingValues ->
        ImageRoll(
            uiState,
            pagerState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.padding(paddingValues)
        )
        if (openAlertDialog.value){
            val currentUri = uiState.value.photoList[pagerState.currentPage]
            ConfirmDeleteDialog(
                onDismissClicked = {
                    openAlertDialog.value = false
                },
                onConfirmClicked = {
                    //TODO This action crashes the app when trying to delete
                    //galleryViewModel.requestTrashPhoto(currentUri){}
                }
            )
        }
    }
    Log.d("Mati", "${uiState.value.photoList.size} photos left")
}


@Composable
fun ImageRoll(
    uiState: State<GalleryUiState>,
    pagerState: PagerState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
){
    var scale by remember { mutableFloatStateOf(1f)}
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isZoomed by remember { mutableStateOf(false) }
    with(sharedTransitionScope){
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = modifier
                .fillMaxSize()
                .background(color = Color.Black)
                .pointerInput(Unit) {
                    //Este detect transform gestures va aca por que si no sobreescribe el gesto de swipe para pasar de foto
                    //del pager, entonces si lo pongo aca funciona bien
                    detectTransformGestures { _, pan, zoom, _ ->
                        //el coerceIn lo que hace es limitar el valor entre los limites que yo le pongo
                        scale = (scale * zoom).coerceIn(1f, 3f)
                        //esto es para calcular cuanto puedo desplazar la foto una vez tenga el zoom hecho
                        val maxPanX = (size.width * (scale - 1)) / 2
                        val maxPanY = (size.height * (scale - 1)) / 2
                        offset = Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxPanX, maxPanX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxPanY, maxPanY)
                        )
                    }
                }
        ) { page ->
            val mediaItem = uiState.value.photoList[page]
            val isCurrentPage = pagerState.currentPage == page
// Esto es para que haga una animacion cada vez que haya zoom
            val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
            val animatedOffset by animateOffsetAsState(targetValue = offset, label = "offset")
            val context = LocalContext.current
            val imageKey = "media-$page"
            val fullImageKey = "media-${mediaItem}"
            //Este launched effect es para que protegerme de un bucle infinito, es decir que
            //se va a activar esta parte del codigo cuando el scale sufra algun cambio
            LaunchedEffect(scale) {
                isZoomed = scale > 1f
            }
            if (mediaItem.isVideo){
                VideoPlayer(
                    uri = mediaItem.uri,
                    isPlaying = isCurrentPage,
                    modifier = Modifier.sharedElement(sharedContentState = rememberSharedContentState
                        (
                        key = "media-$page"
                    ),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaItem.uri)
                        .placeholderMemoryCacheKey(imageKey)
                        .memoryCacheKey(fullImageKey)
                        .build(),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxSize()
                        //aca le digo a la gpu que animaciones hacer en los cambio de escala
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffset.x,
                            translationY = animatedOffset.y
                        )
                        //El pointer input es lo que detecta los toques en la pantalla, en este caso
                        //dentro uso el detect tap gestures para que detecte el double tap
                        //luego dentro tengo la logica para los limites de pantalla
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2f
                                        val targetOffsetX = (size.width / 2 - it.x) * scale
                                        val targetOffsetY = (size.height / 2 - it.y) * scale
                                        offset = Offset(targetOffsetX, targetOffsetY)
                                    }
                                }
                            )
                        }
                        .sharedElement(sharedContentState = rememberSharedContentState
                            (
                            key = "media-$page"
                                    ),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    ,contentScale = ContentScale.Fit
                )
            }
        }
    }

}

@Composable
fun BottomToolbar(
    onDeleteClicked: () -> Unit,
    onSharedClicked: () -> Unit,
    onBackFabClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    checkPhotoIsUploaded: () -> Unit,
    modifier: Modifier = Modifier
){
    BottomAppBar(
        actions = {
            IconButton(onClick = onDeleteClicked) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete"
                )
            }
            IconButton(onClick = onSharedClicked) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share"
                )
            }
            IconButton(onClick = onUploadClicked) {
                Icon(
                    painterResource(R.drawable.outline_cloud_upload_24),
                    contentDescription = "Upload"
                )
            }
            IconButton(onClick = checkPhotoIsUploaded) {
                Icon(
                    painterResource(R.drawable.outline_cloud_alert_24),
                    "Check if file is in the server"
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onBackFabClicked) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun ConfirmDeleteDialog(
    modifier: Modifier = Modifier,
    onDismissClicked: () -> Unit,
    onConfirmClicked: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismissClicked,
        dismissButton = {
           TextButton(
               onClick = onDismissClicked
           ) {
               Text(
                   text = "Return"
               )
           }
                           },
        confirmButton = {
            TextButton(
                onClick = onConfirmClicked
            ) {
                Text(
                    text = "Delete"
                )
            }
        },
        title = {
            Text(
                text = "Confirm delete?"
            )
        },
        text = {
            Text(
                text = "This action cannot be undone"
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = ""
            )
        },
        modifier = modifier
    )
}
@Composable
fun VideoPlayer(
    uri: Uri,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
){
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    val lifeCycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(isPlaying) {
        if (isPlaying){
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }
    DisposableEffect(lifeCycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver{ _, event ->
            when (event){
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                else -> {}
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

//@Preview(showSystemUi = true)
//@Composable
//private fun ImageScreenPreview(){
//    val galleryViewModel: GalleryViewModel = viewModel()
//    ImageScreen(
//        galleryViewModel = galleryViewModel,
//        initialPageIndex = 1
//)
//}

private fun sharePhoto(context: Context, uri: Uri){
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/*"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    val appChooser = Intent.createChooser(shareIntent, "Share on...")
    context.startActivity(appChooser)
}

@Preview
@Composable
private fun BottomToolbarPreview(){
    BottomToolbar(
        onDeleteClicked = {},
        onSharedClicked = {},
        onBackFabClicked = {},
        onUploadClicked = {},
        checkPhotoIsUploaded = {}
    )
}

@Preview(showSystemUi = true)
@Composable
private fun ConfirmDeleteDialogPreview(){
    ConfirmDeleteDialog(
        onDismissClicked = {},
        onConfirmClicked = {}
    )
}