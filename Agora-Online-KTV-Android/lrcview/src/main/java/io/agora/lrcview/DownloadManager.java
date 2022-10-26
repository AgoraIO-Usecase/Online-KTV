package io.agora.lrcview;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadManager {

    private static final String CACHE_FOLDER = "assets";
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private static DownloadManager instance;

    public static DownloadManager getInstance() {
        if(instance == null){
            instance = new DownloadManager();
        }
        return instance;
    }

    private DownloadManager() {}

    @Nullable
    public void download(Context context, String url, FileDownloadSuccessCallback callback, FileDownloadFailureCallback error){
        File folder = new File(context.getExternalCacheDir(), CACHE_FOLDER);
        if(!folder.exists()){
            folder.mkdir();
        }

        File file = new File(folder, url.substring(url.lastIndexOf("/") + 1));
        if (file.exists()) {
            callback.onSuccess(file);
            return;
        }
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                error.onFailed(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) {
                    error.onFailed((Exception) new Throwable("body is empty"));
                    return;
                }

                long total = body.contentLength();

                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = body.byteStream();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        Log.d("down", file.getName() + ", progress: " + progress);
                    }
                    fos.flush();
                    // 下载完成
                    Log.d("down", file.getName() + " onComplete");
                    callback.onSuccess(file);
                } catch (Exception e) {
                    error.onFailed(e);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    public boolean clearCache(Context context){
        File folder = new File(context.getExternalCacheDir(), CACHE_FOLDER);
        return deleteDir(folder);
    }

    private boolean deleteDir(File dir) {
        assert dir != null;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public boolean clearFile(Context context, String fileName){
        File folder = new File(context.getExternalCacheDir(), CACHE_FOLDER);
        if(folder.isDirectory() && folder.exists()){
            File file = new File(folder, fileName);
            file.deleteOnExit();
            return true;
        }
        return false;
    }

    public interface FileDownloadSuccessCallback {
        void onSuccess(File file);
    }

    public interface FileDownloadFailureCallback {
        void onFailed(Exception exception);
    }
}

