package com.example.homealbum.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
    galleryViewModel: GalleryViewModel = viewModel(),
    onSettingsFabClicked: () -> Unit
){
    Scaffold(
        topBar = {GalleryTopBar()},
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {SettingsFab(
            onSettingsFabClicked
        )}
    ) { innerPadding ->
        GalleryGrid(
            galleryViewModel = galleryViewModel,
            modifier = Modifier.padding(innerPadding)
        )
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
            Image(
                painter = painterResource(item.photoRes),
                contentDescription = stringResource(item.descriptionRes),
                modifier = Modifier.height(150.dp),
                contentScale = ContentScale.Crop
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

@Preview(showSystemUi = true)
@Composable
private fun GalleryScreenPreview(){
    GalleryScreen(
        onSettingsFabClicked = {}
    )
}
