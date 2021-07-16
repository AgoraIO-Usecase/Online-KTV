package io.agora.lrcview.bean;

import java.util.List;

/**
 * 歌曲信息
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public class LrcData {
    private IEntry.Type type;
    private List<IEntry> entrys;

    public LrcData(IEntry.Type type) {
        this.type = type;
    }

    public IEntry.Type getType() {
        return type;
    }

    public void setType(IEntry.Type type) {
        this.type = type;
    }

    public List<IEntry> getEntrys() {
        return entrys;
    }

    public void setEntrys(List<IEntry> entrys) {
        this.entrys = entrys;
    }
}
