package me.devsaki.hentoid.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;

import me.devsaki.hentoid.notification.DownloadCompleteNotification;
import me.devsaki.hentoid.notification.DownloadFailedNotification;
import me.devsaki.hentoid.notification.DownloadProgressNotification;
import me.devsaki.hentoid.util.notification.ServiceNotificationManager;
import timber.log.Timber;

import static java.util.Objects.requireNonNull;
import static me.devsaki.hentoid.notification.DownloadProgressNotification.INDETERMINATE;

/**
 * Service responsible for downloading an update APK.
 * Does not support targetSdkVersion > 23 due to exposure of "file:" URI
 *
 * @see UpdateCheckService
 */
public class UpdateDownloadService extends Service implements DownloadStatusListenerV1 {

    private static final int NOTIFICATION_ID = 1;

    private static boolean running;

    public static Intent makeIntent(Context context, String updateUrl) {
        Intent intent = new Intent(context, UpdateDownloadService.class);
        intent.setData(Uri.parse(updateUrl));
        return intent;
    }

    public static boolean isRunning() {
        return running;
    }

    private ThinDownloadManager downloadManager;

    private ServiceNotificationManager notificationManager;

    private Handler progressHandler;

    private int progress;

    @Override
    public void onCreate() {
        running = true;
        downloadManager = new ThinDownloadManager();
        notificationManager = new ServiceNotificationManager(this, NOTIFICATION_ID);
        progressHandler = new Handler();
        Timber.w("Service created");
    }

    @Override
    public void onDestroy() {
        running = false;
        downloadManager.release();
        Timber.w("Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri updateUri = requireNonNull(intent.getData());
        downloadUpdate(updateUri);
        return START_NOT_STICKY;
    }

    private void downloadUpdate(Uri updateUri) {
        Timber.w("Starting download");

        notificationManager.startForeground(new DownloadProgressNotification(INDETERMINATE));

        File apkFile = new File(getExternalCacheDir(), "hentoid.apk");
        Uri destinationUri = Uri.fromFile(apkFile);

        DownloadRequest downloadRequest = new DownloadRequest(updateUri)
                .setDestinationURI(destinationUri)
                .setPriority(DownloadRequest.Priority.HIGH)
                .setDeleteDestinationFileOnFailure(false)
                .setStatusListener(this);

        downloadManager.add(downloadRequest);
        updateNotificationProgress();
    }

    @Override
    public void onDownloadComplete(DownloadRequest downloadRequest) {
        Timber.w("Download complete");
        progressHandler.removeCallbacksAndMessages(null);
        stopForeground(true);
        stopSelf();

        Uri apkUri = downloadRequest.getDestinationURI();
        notificationManager.notify(new DownloadCompleteNotification(apkUri));
    }

    @Override
    public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
        Timber.w("Download failed. code: %s message: %s", errorCode, errorMessage);
        progressHandler.removeCallbacksAndMessages(null);
        stopForeground(true);
        stopSelf();

        Uri downloadUri = downloadRequest.getUri();
        notificationManager.notify(new DownloadFailedNotification(downloadUri));
    }

    /**
     * Only sets {@link UpdateDownloadService#progress}.
     * Actual progress update is done in {@link UpdateDownloadService#updateNotificationProgress()}.
     */
    @Override
    public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
        this.progress = progress;
    }

    private void updateNotificationProgress() {
        Timber.w("Download progress: %s", progress);
        notificationManager.notify(new DownloadProgressNotification(progress));
        progressHandler.postDelayed(this::updateNotificationProgress, 1000);
    }
}
