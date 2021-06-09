package com.agora.data.provider;

public interface IDataProvider {
    void initConfigSource();

    IConfigSource getConfigSource();

    void initStoreSource();

    IStoreSource getStoreSource();

    void initMessageSource();

    IMessageSource getMessageSource();
}
