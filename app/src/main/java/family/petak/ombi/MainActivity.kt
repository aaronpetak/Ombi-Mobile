package family.petak.ombi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import family.petak.ombi.ui.navigation.OmbiNavGraph
import family.petak.ombi.ui.theme.OmbiTheme

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
