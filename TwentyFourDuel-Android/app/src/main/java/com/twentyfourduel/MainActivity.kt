package com.twentyfourduel

import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.twentyfourduel.model.GameState
import com.twentyfourduel.model.HistoryStore
import com.twentyfourduel.sound.SoundManager
import com.twentyfourduel.ui.AppColors
import com.twentyfourduel.ui.GameScreen

class MainActivity : ComponentActivity() {
    private val game: GameState by viewModels()
    private var soundManager: SoundManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        soundManager = SoundManager(this)
        game.soundManager = soundManager
        game.vibrator = getSystemService(Vibrator::class.java)
        game.historyStore = HistoryStore(this)

        soundManager?.startBGM()

        setContent {
            GameScreen(game)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.release()
    }
}
