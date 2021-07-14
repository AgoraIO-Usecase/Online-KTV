package io.agora.lrcview.bean;

import io.agora.lrcview.LrcEntry;

public class LrcEntryDefault implements IEntry {
    private IEntry.Tone[] tones = new Tone[1];

    public LrcEntryDefault(long time, String text) {
        tones[0] = new Tone();
        tones[0].begin = time;
        tones[0].word = text;
        tones[0].lang = Lang.Chinese;
    }

    @Override
    public LrcEntry createLRCEntry() {
        return new LrcEntry(this);
    }

    @Override
    public Type getType() {
        return Type.Default;
    }

    @Override
    public void setDuration(long d) {
        tones[0].end = tones[0].begin + d;
    }

    @Override
    public long getTime() {
        return tones[0].begin;
    }

    @Override
    public Tone[] getTones() {
        return tones;
    }
}
