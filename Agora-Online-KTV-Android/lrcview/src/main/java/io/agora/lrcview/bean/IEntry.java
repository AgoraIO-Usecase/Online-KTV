package io.agora.lrcview.bean;

import io.agora.lrcview.LrcEntry;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public interface IEntry {
    public enum Type {
        Default, Migu;
    }

    Type getType();

    long getTime();

    String getText();

    String[] getTexts();

    float getOffset(long time);

    default LrcEntry createLRCEntry() {
        return new LrcEntry(this);
    }
}
