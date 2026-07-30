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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homealbum.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.homealbum.model.MediaItem
import com.example.homealbum.data.PhotoRepository

//enum class AppScreens{
//    GALLERY_START,
//    SETTINGS,
//    IMAGE_VIEW
//}

//@RequiresApi(Build.VERSION_CODES.Q)
//@Composable
//fun HomeAlbumApp(
//    navController: NavHostController = rememberNavController()
//){
////    val context = LocalContext.current
////    val photoRepository = PhotoRepository(context)
//
//    val galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
//    NavHost(
//        navController = navController,
//        startDestination = AppScreens.GALLERY_START.name,
//        modifier = Modifier.fillMaxSize()
//    ){
//        composable(
//            route = AppScreens.GALLERY_START.name
//        ){
//            GalleryScreen(
//                galleryViewModel = galleryViewModel,
//                onSettingsFabClicked = {
//                    navController.navigate(AppScreens.SETTINGS.name)
//                },
//                onImageClicked = { index ->
//                    navController.navigate(route = "${AppScreens.IMAGE_VIEW.name}/$index")
//                }
//            )
//        }
//        composable(
//            route = AppScreens.SETTINGS.name
//        ){
//            SettingsScreen()
//        }
//        composable(
//            route = "${AppScreens.IMAGE_VIEW.name}/{index}",
//            arguments = listOf(navArgument("index") { type = NavType.IntType })
//        ) { navBackStackEntry ->
//            val index = navBackStackEntry.arguments?.getInt("index") ?: 0
//            ImageScreen(
//                galleryViewModel = galleryViewModel,
//                initialPageIndex = index,
//                onBackFabClicked = {navController.popBackStack()},
//                onLastPhotoDeleted = {navController.popBackStack()}
//                )
//        }
//    }
//}
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onSettingsFabClicked: () -> Unit,
    onImageClicked: (Int) -> Unit
){
    Scaffold(
        topBar = {GalleryTopBar()},
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {SettingsFab(
            onSettingsFabClicked
        )}
    ) { innerPadding ->
        val context = LocalContext.current

        val permissionToRequest = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
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
                onImageClicked = onImageClicked,
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

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun GalleryGrid(
    galleryViewModel: GalleryViewModel,
    onImageClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
){
    val galleryUiState = galleryViewModel.galleryUiState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = galleryUiState.value.photoList,
            key = {_, uri -> uri.toString()}
        ){ index, item ->
            ImageThumbnail(
                mediaItem = item,
                galleryViewModel = galleryViewModel,
                index = index,
                onImageClicked = onImageClicked
            )
        }
    }
}

@Composable
fun SettingsFab(
    onSettingsFabClicked: () -> Unit
){
    FloatingActionButton(
        onClick = onSettingsFabClicked
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = ""
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(){
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name)
            )
        }
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
            text = "Request Permission",
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
    onImageClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
){
    /**
     * Here the thumbnail value is initiated using produceState which makes this code run
     * inside a coroutine, so we can call the getThumbnail method from the viewModel
     */
    val thumbnail by produceState<Bitmap?>(initialValue = null, mediaItem.uri) {
        value = galleryViewModel.getThumbnail(mediaItem, 300, 300)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ){
        AsyncImage(
            model = thumbnail,
            contentDescription = "",
            modifier = Modifier.height(150.dp).clickable(true, onClick = {onImageClicked(index)}),
            contentScale = ContentScale.Crop
        )
        if (mediaItem.isVideo){
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = ""
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



@Preview(showSystemUi = true)
@Composable
private fun RequestPermissionPreview(){
    RequestPermissionFab(
        onRequestPermissionClicked = {}
    )
}
