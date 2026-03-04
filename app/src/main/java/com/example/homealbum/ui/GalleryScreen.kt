package com.example.homealbum.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homealbum.R
import com.example.homealbum.data.GalleryViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.homealbum.data.GalleryViewModelFactory
import com.example.homealbum.model.PhotoRepository

enum class AppScreens{
    GALLERY_START,
    SETTINGS
}

@Composable
fun HomeAlbumApp(
    navController: NavHostController = rememberNavController()
){
    NavHost(
        navController = navController,
        startDestination = AppScreens.GALLERY_START.name,
        modifier = Modifier.fillMaxSize()
    ){
        composable(
            route = AppScreens.GALLERY_START.name
        ){
            GalleryScreen(
                onSettingsFabClicked = {
                    navController.navigate(AppScreens.SETTINGS.name)
                }
            )
        }
        composable(
            route = AppScreens.SETTINGS.name
        ){
            SettingsScreen()
        }
    }
}
@Composable
fun GalleryScreen(
    //galleryViewModel: GalleryViewModel = viewModel(),
    onSettingsFabClicked: () -> Unit
){
    Scaffold(
        topBar = {GalleryTopBar()},
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {SettingsFab(
            onSettingsFabClicked
        )}
    ) { innerPadding ->
        val context = LocalContext.current
        val photoRepository = PhotoRepository(context)

        val factory = GalleryViewModelFactory(photoRepository)
        val galleryViewModel: GalleryViewModel = viewModel(factory = factory)

        val permissionToRequest = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
            )
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {isGranted ->
                hasPermission = isGranted
                if(isGranted){
                    galleryViewModel.loadPhotos()
                }
            }
        )

        LaunchedEffect(Unit) {
            if (!hasPermission){
                permissionLauncher.launch(permissionToRequest)
            } else {
                galleryViewModel.loadPhotos()
            }
        }

        if(hasPermission){
            GalleryGrid(
                galleryViewModel = galleryViewModel,
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

@Composable
private fun GalleryGrid(
    galleryViewModel: GalleryViewModel,
    modifier: Modifier = Modifier
){
    val gameUiState = galleryViewModel.galleryUiState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(gameUiState.value.photoList){ item ->
            AsyncImage(
                model = item,
                contentDescription = "",
                modifier = Modifier.height(150.dp),
                contentScale = ContentScale.Crop,
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
