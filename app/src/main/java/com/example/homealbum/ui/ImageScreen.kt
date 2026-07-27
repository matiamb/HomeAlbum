package com.example.homealbum.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.homealbum.data.GalleryUiState
import com.example.homealbum.data.GalleryViewModel

@Composable
fun ImageScreen(
    galleryViewModel: GalleryViewModel,
    initialPageIndex: Int,
    onBackFabClicked: () -> Unit,
    onLastPhotoDeleted: () -> Unit,
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
    //val currentUri = uiState.value.photoList[pagerState.currentPage]
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {result ->
        if (result.resultCode == Activity.RESULT_OK){
            val currentUri = uiState.value.photoList[pagerState.currentPage]
            val photoCount = uiState.value.photoList.size
            if (photoCount == 1){
                galleryViewModel.removeThrashedPhotoFromUi(currentUri)
                onLastPhotoDeleted()
            } else {
                galleryViewModel.removeThrashedPhotoFromUi(currentUri)
            }
        }
    }
    Scaffold(
        bottomBar = { BottomToolbar(
            onDeleteClicked = {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){
                    openAlertDialog.value = true
                }else{
                    val currentUri = uiState.value.photoList[pagerState.currentPage]
                    galleryViewModel.requestTrashPhoto(currentUri){ intentSenderRequest ->
                        deleteLauncher.launch(intentSenderRequest)
                    }
                }
                              },
            onSharedClicked = {
                val currentUri = uiState.value.photoList[pagerState.currentPage]
                sharePhoto(context =context, uri = currentUri )
                              },
            onBackFabClicked = onBackFabClicked
        ) },
        modifier = Modifier
    ) { paddingValues ->
        ImageRoll(
//            galleryViewModel = galleryViewModel,
//            initialPageIndex,
            uiState,
            pagerState,
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
//    galleryViewModel: GalleryViewModel,
//    initialPageIndex: Int,
    uiState: State<GalleryUiState>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
){
//    val uiState = galleryViewModel.galleryUiState.collectAsState()
//    val pagerState = rememberPagerState(
//        initialPage = initialPageIndex,
//        pageCount = {uiState.value.photoList.size}
//    )
    var scale by remember { mutableFloatStateOf(1f)}
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isZoomed by remember { mutableStateOf(false) }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !isZoomed,
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.inverseOnSurface)
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
        val uri = uiState.value.photoList[page]
        val context = LocalContext.current
        val isCurrentPage = pagerState.currentPage == page
//        var scale by remember { mutableFloatStateOf(1f)}
//        var offset by remember { mutableStateOf(Offset.Zero) }
// Esto es para que haga una animacion cada vez que haya zoom
        val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
        val animatedOffset by animateOffsetAsState(targetValue = offset, label = "offset")
        //Este launched effect es para que protegerme de un bucle infinito, es decir que
        //se va a activar esta parte del codigo cuando el scale sufra algun cambio
        LaunchedEffect(scale) {
               isZoomed = scale > 1f
        }
        if (isVideoUri(context, uri)){
            VideoPlayer(
                uri = uri,
                isPlaying = isCurrentPage
            )
        } else {
            AsyncImage(
                model = uri,
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
//                .pointerInput(Unit) {
//                    //Esto no termina de funcionar, el pinch to zoom solo funciona si hago el doble tap antes
//                    detectTransformGestures { _, pan, zoom, _ ->
//                        scale = (scale * zoom).coerceIn(1f, 3f)
//                        val maxPanX = (size.width * (scale - 1)) / 2
//                        val maxPanY = (size.height * (scale - 1)) / 2
//                        offset = Offset(
//                            x = (offset.x + pan.x * scale).coerceIn(-maxPanX, maxPanX),
//                            y = (offset.y + pan.y * scale).coerceIn(-maxPanY, maxPanY)
//                        )
//                    }
//                },
                ,contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun BottomToolbar(
    onDeleteClicked: () -> Unit,
    onSharedClicked: () -> Unit,
    onBackFabClicked: () -> Unit,
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onBackFabClicked) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
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
        }
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

private fun isVideoUri(context: Context, uri: Uri): Boolean{
    val mimeType = context.contentResolver.getType(uri)
    return  mimeType?.startsWith("video/") == true
}
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
        onBackFabClicked = {}
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