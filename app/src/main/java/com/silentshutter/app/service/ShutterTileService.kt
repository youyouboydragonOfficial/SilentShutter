package com.silentshutter.app.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.silentshutter.app.manager.ShutterMuteManager

@RequiresApi(Build.VERSION_CODES.N)
class ShutterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val newState = !ShutterMuteManager.isMasterEnabled.value
        ShutterMuteManager.setMasterEnabled(newState)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = ShutterMuteManager.isMasterEnabled.value

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isEnabled) "Silent: ON" else "Silent: OFF"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEnabled) "Camera Mute Ready" else "Standby"
        }
        tile.updateTile()
    }
}
