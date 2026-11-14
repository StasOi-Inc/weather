package ru.rufp.weaser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import ru.rufp.weaser.presentation.AppRoot
import ru.rufp.weaser.presentation.theme.WeaserTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeaserTheme {
                AppRoot()
            }
        }
    }
}
