package com.vidovichb.ifunny_downloader;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

public class iFunnyDownloadService extends IntentService {
    private long downloadID;

    public iFunnyDownloadService() {
        super("iFunnyDownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String data_url = intent.getStringExtra("data_url");

        //Log.d("IFUNNYDEBUG",""+data_url);
        boolean isImage = data_url.contains("picture");

        handleDownloading(data_url, isImage);

        stopSelf();
    }

    private void handleDownloading(String data_url, boolean isImage) {
        String source_url = parseContent(data_url, isImage);

        if(isImage){
            downloadFile("ifunnyImage.jpg", source_url);
        }else{
            downloadFile("ifunnyVideo.mp4", source_url);
        }
    }

    private void downloadFile(String fileName, String source_url){
        // fileName -> fileName with extension
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(source_url))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName);
        DownloadManager downloadManager =  (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
    }

    private String parseContent(String data_url, boolean isImage){
        Document document = null;
        try {
            document = Jsoup.connect(data_url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //production
        String source_url = null;

        if(isImage){
            //finds images ending with .webp
            Element media = document.select("img[src~=(?i)\\.(png|jpe?g|gif|webp)]").first();
            source_url = media.attr("src");
        }else{
            //finds videos ending with .mp4
            Element media = document.select("video[data-src~=(?i)\\.mp4]").first();
            source_url = media.attr("data-src");
        }

        return source_url;

        //end production

        /*  ====DEBUGGING====
        Elements media;

        if(isImage){
            //finds images ending with .webp
            media = document.select("img[src~=(?i)\\.(png|jpe?g|gif|webp)]");
        }else{
            //finds videos ending with .mp4
            media = document.select("video[data-src~=(?i)\\.mp4]");
        }

        Log.d("IFUNNYDEBUG", ""+media.size());
        for (Element m: media) {
            Log.d("IFUNNYDEBUG", ""+m);
        }
         */
    }
}