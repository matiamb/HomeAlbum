package com.example.homealbum.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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

//    LazyRow(
//        modifier = modifier.fillMaxSize()
//    ) {
////        items(uiState.value.photoList){item ->
////            AsyncImage(
////                model = item,
////                contentDescription = "",
////                modifier = Modifier.fillMaxWidth()
////            )
////        }
//        items(imageList){item ->
//            Image(
//                painter = painterResource(item.photoRes),
//                contentDescription = "",
//                modifier.fillMaxSize(),
//                contentScale = ContentScale.FillWidth
//            )
//        }
//    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.inverseOnSurface)
    ) { page ->
        val uri = uiState.value.photoList[page]
        var isZoomed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isZoomed) 3f else 1f,
            label = "zoom_animation"
        )
        AsyncImage(
            model = uri,
            contentDescription = "",
            modifier = Modifier.fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
                .pointerInput(Unit){
                    detectTapGestures(
                        onDoubleTap = {
                            isZoomed = !isZoomed
                        }
                    )
                },
            contentScale = ContentScale.Fit
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