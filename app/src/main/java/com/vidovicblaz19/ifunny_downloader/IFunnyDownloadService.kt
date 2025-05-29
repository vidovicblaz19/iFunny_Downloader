package com.vidovicblaz19.ifunny_downloader

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class IFunnyDownloadService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID = "ifunny_download_channel"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_NAME = "iFunny Download Service"
        const val EXTRA_SHARED_TEXT = "shared_text"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedText = intent?.getStringExtra(EXTRA_SHARED_TEXT) ?: ""

        // Start foreground service immediately
        startForeground(NOTIFICATION_ID, createProcessingNotification("Initialized media download"))

        coroutineScope.launch {
            try {
                processDownload(sharedText)
            } catch (e: Exception){
//                Log.d("IFunnyDownloadService", "Download failed: ${e.message}")
                showNotification("Download failed: ${e.message}")
            } finally {
//                The whole program should end when we stop the services.

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        // If killed, do not recreate until explicitly started
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ifunny downloader progress updates."
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createProcessingNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iFunny Downloader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iFunny Downloader")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun processDownload(text: String) {
        val urlRegex = Regex("""https?://\S+""")
        val essence = urlRegex.find(text)?.value ?: throw IllegalArgumentException("Unable to find URL.")

        val allVideos = fetchAndParseHTML(essence)

        val request = DownloadManager.Request(Uri.parse(allVideos.first()))
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, "ifunnyVideo.mp4")

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun fetchAndParseHTML(url: String): List<String> {
        val doc = Jsoup.connect(url).timeout(5000).get()
        val videos = mutableListOf<String>()

        doc.select("video[src]").forEach {
            videos.add(it.attr("abs:src"))
        }

        doc.select("video[data-src]").forEach {
            videos.add(it.attr("abs:data-src"))
        }

        doc.select("source[src]").forEach {
            videos.add(it.attr("abs:src"))
        }
        return videos
    }
}