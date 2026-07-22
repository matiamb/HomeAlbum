package com.example.homealbum.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homealbum.data.GalleryViewModel
import com.example.homealbum.data.ImageDataSource.imageList

@Composable
fun ImageScreen(
    galleryViewModel: GalleryViewModel,
    initialPageIndex: Int,
    modifier: Modifier = Modifier
){
    ImageRoll(
        galleryViewModel = galleryViewModel,
        initialPageIndex
    )
}

@Composable
fun ImageRoll(
    galleryViewModel: GalleryViewModel,
    initialPageIndex: Int,
    modifier: Modifier = Modifier
){
    val uiState = galleryViewModel.galleryUiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = {uiState.value.photoList.size}
    )
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

@Preview(showSystemUi = true)
@Composable
private fun ImageScreenPreview(){
    //val galleryViewModel: GalleryViewModel = viewModel()
//    ImageScreen(
//    //galleryViewModel = galleryViewModel
//)
}