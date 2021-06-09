package com.agora.data.observer;

import android.content.Context;

import androidx.annotation.NonNull;

import com.agora.data.BaseError;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/**
 * 网络回调基类，主要负责异常封装，以及数据序列化。
 *
 * @author Aslan
 * @date 2019/9/23
 */
public abstract class DataSingleObserver<T> implements SingleObserver<T> {

    private Context context;

    public DataSingleObserver(Context context) {
        this.context = context;
    }

    @Override
    public void onError(@NonNull Throwable e) {
        if (e instanceof BaseError) {
            handleError((BaseError) e);
        } else {
            handleError(new BaseError(-100, e.getMessage()));
        }
    }

    @Override
    public void onSuccess(@NonNull T t) {
        handleSuccess(t);
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {

    }

    public abstract void handleError(@NonNull BaseError e);

    public abstract void handleSuccess(@NonNull T t);
}