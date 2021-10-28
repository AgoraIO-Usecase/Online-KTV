package io.agora.ktv.repo;

import android.util.Log;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import io.agora.ktv.manager.UserManager;
import com.agora.data.provider.IDataRepository;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DataRepositoryImpl implements IDataRepository {
    private static DataRepositoryImpl instance;
    private final OkHttpClient okHttpClient;

    private DataRepositoryImpl() {
        okHttpClient = new OkHttpClient.Builder().build();
    }

    public static DataRepositoryImpl getInstance() {
        if (instance == null) {
            synchronized (DataRepositoryImpl.class) {
                if (instance == null)
                    instance = new DataRepositoryImpl();
            }
        }
        return instance;
    }

    @Override
    public Observable<User> login(int userId, String userName) {
        User user = new User();
        user.setUserId(userId);
        user.setAvatar(UserManager.randomAvatar());
        user.setName(userName);
        return Observable.just(user);
    }

    @Override
    public Observable<List<MusicModel>> getMusics(@Nullable String searchKey) {
        List<MusicModel> res;
        if (searchKey == null || searchKey.isEmpty())
            res = ExampleData.exampleSongs;
        else {
            res = new ArrayList<>();
            MusicModel music;

            int size = ExampleData.exampleSongs.size();
            for (int i = 0; i < size; i++) {
                music = ExampleData.exampleSongs.get(i);
                if (music.getName().contains(searchKey) || music.getSinger().contains(searchKey))
                    res.add(music);
            }
        }
        return Observable.just(res);
    }

    @Override
    public Observable<MusicModel> getMusic(@NotNull String musicId) {
        for (int i = 0; i < ExampleData.exampleSongs.size(); i++) {
            if (ExampleData.exampleSongs.get(i).getMusicId().equals(musicId)) {
                return Observable.just(ExampleData.exampleSongs.get(i));
            }
        }
        return Observable.error(new Throwable("Can not find this song."));
    }

    @Override
    public Completable download(@NotNull File file, @NotNull String url) {
        return Completable.create(emitter -> {
            Log.d("down", file.getName() + ", url: " + url);

            if (file.isDirectory()) {
                emitter.onError(new Throwable("file is a Directory"));
                return;
            }

            Request request = new Request.Builder().url(url).build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.onError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        emitter.onError(new Throwable("body is empty"));
                        return;
                    }

                    long total = body.contentLength();

                    if (file.exists() && file.length() == total) {
                        emitter.onComplete();
                        return;
                    }

                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len;
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
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                            if (fos != null)
                                fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
    }

    @Override
    public Observable<List<AgoraRoom>> getRooms() {
        return Observable.just(ExampleData.exampleRooms);
    }
}
