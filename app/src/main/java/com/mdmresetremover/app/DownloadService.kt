package com.mdmresetremover.app

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground service handled here if needed for long downloads
        return START_NOT_STICKY
    }
}