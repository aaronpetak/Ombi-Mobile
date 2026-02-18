package com.ombi.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.ombi.mobile.ui.navigation.OmbiNavGraph
import com.ombi.mobile.ui.theme.OmbiTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmbiTheme {
                OmbiNavGraph()
            }
        }
    }
}
