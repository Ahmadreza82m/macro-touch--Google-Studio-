package com.example.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class MacroQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_ACTIVE) {
            // Stop service
            val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_STOP_ALL
            }
            startService(stopIntent)
            tile.state = Tile.STATE_INACTIVE
        } else {
            // Start overlay or open app if permissions needed
            val overlayIntent = Intent(this, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.label = "Macro Touch"
        tile.state = if (TouchAutomationService.isServiceActive.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
