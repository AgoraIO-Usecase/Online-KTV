package com.agora.data.provider;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import cn.leancloud.LCObject;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

class AVObjectToObservable<T> implements Function<LCObject, Observable<T>> {

    private Type type;

    public AVObjectToObservable(Type type) {
        this.type = type;
    }

    @Override
    public Observable<T> apply(LCObject respone) throws Exception {
        T data = new Gson().fromJson(respone.toJSONObject().toJSONString(), type);
        return Observable.just(data);
    }
}
