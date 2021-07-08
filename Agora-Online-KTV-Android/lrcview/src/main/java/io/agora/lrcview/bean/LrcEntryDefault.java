package io.agora.lrcview.bean;

import io.agora.lrcview.LrcEntry;

public class LrcEntryDefault implements IEntry {
    public long time;
    public String text;

    public String[] texts;

    public LrcEntryDefault(long time, String text) {
        this.time = time;
        this.text = text;
    }

    @Override
    public LrcEntry createLRCEntry() {
        texts = new String[]{text};
        return new LrcEntry(this);
    }

    @Override
    public Type getType() {
        return Type.Default;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String[] getTexts() {
        return texts;
    }

    @Override
    public float getOffset(long time) {
        return 1;
    }
}
