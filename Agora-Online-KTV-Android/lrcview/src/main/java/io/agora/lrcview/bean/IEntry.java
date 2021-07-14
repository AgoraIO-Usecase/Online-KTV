package io.agora.lrcview.bean;

import io.agora.lrcview.LrcEntry;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public interface IEntry {
    enum Type {
        Default, Migu;
    }

    Type getType();

    void setDuration(long d);

    /**
     * 第一个歌词时间
     *
     * @return
     */
    long getTime();

    Tone[] getTones();

    default LrcEntry createLRCEntry() {
        return new LrcEntry(this);
    }

    enum Lang {
        Chinese, English
    }

    class Tone {
        public long begin;
        public long end;
        public String word;
        public Lang lang = Lang.Chinese;
    }
}
