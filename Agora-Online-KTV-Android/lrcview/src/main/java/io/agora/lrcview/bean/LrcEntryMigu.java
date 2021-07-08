package io.agora.lrcview.bean;

import java.util.List;

import io.agora.lrcview.LrcEntry;

public class LrcEntryMigu implements IEntry {
    @Override
    public Type getType() {
        return Type.Migu;
    }

    @Override
    public long getTime() {
        Tone first = tones.get(0);
        return (long) (first.begin * 1000L);
    }

    private String text;
    private String[] texts;

    @Override
    public String[] getTexts() {
        return texts;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public LrcEntry createLRCEntry() {
        StringBuilder sb = new StringBuilder();
        texts = new String[tones.size()];
        for (int i = 0; i < tones.size(); i++) {
            Tone tone = tones.get(i);
            texts[i] = tone.word;
            sb.append(tone.word);

            if (!"1".equals(tone.lang)) {
                //英文模式下，词之间增加空格
                texts[i] = texts[i] + " ";
                sb.append(" ");
            }
        }
        text = sb.toString();
        return new LrcEntry(this);
    }

    @Override
    public float getOffset(long time) {
        float curTime = tones.get(0).begin;
        float dur = tones.get(tones.size() - 1).end - tones.get(0).begin;
        float pct = ((time / 1000F - curTime)) / dur;
        if (pct < 0)
            pct = 0;
        if (pct > 1)
            pct = 1;
        return pct;

//        int done = 0;
//        float percent2 = 0;
//        for (Tone tone : tones) {
//            if (time >= (tone.end * 1000)) {
//                done++;
//            } else {
//                percent2 = (time / 1000F - tone.begin) / (tone.end - tone.begin);
//                break;
//            }
//        }
//
//        float percent1 = done / (float) tones.size();
//        float pct = percent1 + percent2;
//        if (pct < 0)
//            pct = 0;
//        if (pct > 1)
//            pct = 1;
//        return pct;
    }

    public enum Mode {
        Default, Woman, Man
    }

    public Mode mode;
    public List<Tone> tones;

    public static class Tone {
        public float begin;
        public float end;
        public int pitch;
        public String pronounce;
        public String lang;
        public String word;
    }
}
