package com.agora.data.provider2;

import androidx.annotation.NonNull;

import com.agora.data.model.User;

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
}
