package com.agora.data.provider;

import androidx.annotation.NonNull;

import com.agora.data.model.MusicModel;
import com.agora.data.model.User;

import java.util.List;

import io.reactivex.Observable;

/**
 * 数据仓库接口
 *
 * @author chenhengfei(Aslanchen)
 */
public interface IDataRepositroy {
    Observable<User> login(@NonNull User user);

    Observable<User> update(@NonNull User user);

    Observable<User> getUser(@NonNull String userId);

    Observable<List<MusicModel>> getMusics();
}
