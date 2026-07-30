package com.example.homealbum.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

enum class AppScreens{
    GALLERY_START,
    SETTINGS,
    IMAGE_VIEW
}
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeAlbumApp(
    navController: NavHostController = rememberNavController()
){
//    val context = LocalContext.current
//    val photoRepository = PhotoRepository(context)

    val galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
    NavHost(
        navController = navController,
        startDestination = AppScreens.GALLERY_START.name,
        modifier = Modifier.fillMaxSize()
    ){
        composable(
            route = AppScreens.GALLERY_START.name
        ){
            GalleryScreen(
                galleryViewModel = galleryViewModel,
                onSettingsFabClicked = {
                    navController.navigate(AppScreens.SETTINGS.name)
                },
                onImageClicked = { index ->
                    navController.navigate(route = "${AppScreens.IMAGE_VIEW.name}/$index")
                }
            )
        }
        composable(
            route = AppScreens.SETTINGS.name
        ){
            SettingsScreen()
        }
        composable(
            route = "${AppScreens.IMAGE_VIEW.name}/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { navBackStackEntry ->
            val index = navBackStackEntry.arguments?.getInt("index") ?: 0
            ImageScreen(
                galleryViewModel = galleryViewModel,
                initialPageIndex = index,
                onBackFabClicked = {navController.popBackStack()},
                onLastPhotoDeleted = {navController.popBackStack()}
            )
        }
    }
}