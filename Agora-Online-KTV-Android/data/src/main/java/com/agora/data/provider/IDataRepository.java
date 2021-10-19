package com.agora.data.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import java.io.File;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * 数据仓库接口
 *
 * @author chenhengfei(Aslanchen)
 */
public interface IDataRepository {
    Observable<User> login(int userId, String userName);

    Observable<List<MusicModel>> getMusics(@Nullable String searchKey);

    Observable<MusicModel> getMusic(@NonNull String musicId);

    Observable<List<AgoraRoom>> getRooms();

    Completable download(@NonNull File file, @NonNull String url);
}
