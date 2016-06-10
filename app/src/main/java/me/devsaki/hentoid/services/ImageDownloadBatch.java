package me.devsaki.hentoid.services;

import android.webkit.CookieManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import me.devsaki.hentoid.util.Consts;
import me.devsaki.hentoid.util.Helper;
import me.devsaki.hentoid.util.LogHelper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Shiro on 3/28/2016.
 * Handles image download tasks and batch operations
 * Intended to have default access level for use with DownloadService class only
 * <p/>
 * TODO: Test timeout handling:
 * {@link ImageDownloadBatch#newTask}
 * Ref: https://goo.gl/OF86un
 */
final class ImageDownloadBatch {

    private static final String TAG = LogHelper.makeLogTag(ImageDownloadBatch.class);
    private static final int BUFFER_SIZE = 10 * 1024;
    private static final CookieManager cookieManager = CookieManager.getInstance();
    private static OkHttpClient client = new OkHttpClient();
    private final Semaphore semaphore = new Semaphore(0);
    private boolean hasError = false;
    private short errorCount = 0;

    void newTask(final File dir, final String filename, final String url) {
        String cookies = cookieManager.getCookie(url);
        if (cookies.isEmpty()) {
            cookies = Helper.getSessionCookie();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Consts.USER_AGENT)
                .addHeader("Cookie", cookies)
                .build();

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request)
                .enqueue(new Callback(dir, filename));
    }

    void waitForOneCompletedTask() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            LogHelper.e(TAG, "Interrupt while waiting on download task completion: ", e);
        }
    }

    void cancelAllTasks() {
        client.dispatcher().cancelAll();
    }

    boolean hasError() {
        return hasError;
    }

    short getErrorCount() {
        return errorCount;
    }

    private class Callback implements okhttp3.Callback {
        private final File dir;
        private final String filename;

        private Callback(final File dir, final String filename) {
            this.dir = dir;
            this.filename = filename;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            LogHelper.e(TAG, "Error downloading image: " + call.request().url(), e);
            hasError = true;
            synchronized (ImageDownloadBatch.this) {
                errorCount++;
            }
        }

        @SuppressWarnings("TryFinallyCanBeTryWithResources")
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            LogHelper.d(TAG, "Start downloading image: " + call.request().url());

            if (!response.isSuccessful()) {
                LogHelper.w(TAG, "Unexpected http status code: " + response.code());
            }

            final File file;
            switch (response.header("Content-Type")) {
                case "image/png":
                    file = new File(dir, filename + ".png");
                    break;
                case "image/gif":
                    file = new File(dir, filename + ".gif");
                    break;
                default:
                    file = new File(dir, filename + ".jpg");
                    break;
            }

            if (file.exists()) {
                return;
            }

            OutputStream output = null;
            final InputStream input = response.body().byteStream();
            final byte[] buffer = new byte[BUFFER_SIZE];
            try {
                output = new FileOutputStream(file);
                int dataLength;
                while ((dataLength = input.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    output.write(buffer, 0, dataLength);
                }
                output.flush();
            } catch (IOException e) {
                if (!file.delete()) {
                    LogHelper.e(TAG, "Failed to delete file: " + file.getAbsolutePath());
                }
                throw e;
            } finally {
                if (output != null) {
                    output.close();
                }
                input.close();
            }

            semaphore.release();
            LogHelper.d(TAG, "Done downloading image: " + call.request().url());
        }
    }
}