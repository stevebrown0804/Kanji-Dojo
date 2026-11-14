package ua.syt0r.kanji.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import org.koin.android.ext.android.inject
import ua.syt0r.kanji.core.theme_manager.ThemeManager

open class KanjiDojoActivity : AppCompatActivity() {

    private val themeManager by inject<ThemeManager>()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        themeManager.invalidate()


        setContent {
            KanjiDojoApp(
                windowSizeClass = calculateWindowSizeClass(this)
            )
        }
    }



}
