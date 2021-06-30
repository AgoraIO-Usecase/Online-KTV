package io.agora.scene.ktv;

import org.springframework.cache.annotation.Cacheable;

public class OnLineResourceService {
    private static final String PREFIX = "http://58.211.16.78/home/devops/web_demo/project/ktv/";
    private final boolean devMode = true;

    @Cacheable(value = "MUSIC",key = "#id")
    public String getMusicUrl(String id){
        if (devMode){
            return PREFIX + id + ".mp3";
        }
        return "404";
    }

    @Cacheable(value = "LRC",key = "#id")
    public String getLrcUrl(String id){
        if (devMode){
            return PREFIX + id + ".xml";
        }
        return "404";
    }

}
