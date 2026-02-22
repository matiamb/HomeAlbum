package com.example.homealbum.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(){
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) {innerPadding ->
        SettingItemsList(
            modifier = Modifier.padding(innerPadding)
        )
    }
}
@Composable
fun SettingItemsList(
    modifier: Modifier = Modifier
){
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        items(16){item ->
            SettingItemCard()
        }
    }
}
@Composable
fun SettingItemCard(
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = Modifier.height(50.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = ""
            )
            Text(
                text = "Setting",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SettingItemCardPreview(){
    SettingsScreen()
}