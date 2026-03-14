package com.sonara.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SonaraService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
}
