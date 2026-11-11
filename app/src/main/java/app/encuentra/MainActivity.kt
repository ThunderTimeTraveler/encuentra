package app.encuentra

import android.os.Build
import android.os.Bundle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import app.encuentra.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            // Los colores del telefono (Android 12+), en claro u oscuro.
            val oscuro = isSystemInDarkTheme()
            val colores = when {
                Build.VERSION.SDK_INT >= 31 && oscuro -> dynamicDarkColorScheme(ctx)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
                oscuro -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = colores) { App() }
        }
    }
}
