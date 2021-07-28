package io.agora.lrcview.bean;

import java.util.List;

/**
 * 歌曲信息
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public class LrcData {
    public static enum Type {
        Default, Migu;
    }

    public Type type;
    public List<LrcEntryData> entrys;

    public LrcData(Type type) {
        this.type = type;
    }
}
