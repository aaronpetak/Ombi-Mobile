package family.petak.ombi.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import family.petak.ombi.data.preferences.UserPreferences
import javax.inject.Inject

/** Minimal ViewModel used only to inject UserPreferences into OmbiNavGraph. */
@HiltViewModel
class NavViewModel @Inject constructor(
    val userPreferences: UserPreferences
) : ViewModel()
