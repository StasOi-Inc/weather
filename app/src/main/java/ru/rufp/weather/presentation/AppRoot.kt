package ru.rufp.weather.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Source
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.rufp.weather.R
import ru.rufp.weather.presentation.screens.AboutScreen
import ru.rufp.weather.presentation.screens.LocationSheet
import ru.rufp.weather.presentation.screens.MainScreen
import ru.rufp.weather.presentation.screens.SourcesScreen

private enum class Tab { NOW, SOURCES, ABOUT }

@Composable
fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val state by mainViewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.NOW) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.NOW,
                    onClick = { tab = Tab.NOW },
                    icon = { Icon(Icons.Filled.CloudQueue, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_now)) }
                )
                NavigationBarItem(
                    selected = tab == Tab.SOURCES,
                    onClick = { tab = Tab.SOURCES },
                    icon = { Icon(Icons.Filled.Source, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_sources)) }
                )
                NavigationBarItem(
                    selected = tab == Tab.ABOUT,
                    onClick = { tab = Tab.ABOUT },
                    icon = { Icon(Icons.Filled.HelpOutline, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_about)) }
                )
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.NOW -> MainScreen(mainViewModel, modifier)
            Tab.SOURCES -> SourcesScreen(modifier)
            Tab.ABOUT -> AboutScreen(modifier)
        }
    }

    if (state.showLocationSheet) {
        LocationSheet(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.closeLocationSheet() }
        )
    }
}
