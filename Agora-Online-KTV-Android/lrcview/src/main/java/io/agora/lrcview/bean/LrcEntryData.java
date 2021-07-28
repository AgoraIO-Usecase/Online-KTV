package io.agora.lrcview.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public class LrcEntryData {

    public enum Lang {
        Chinese, English
    }

    public static class Tone {
        public long begin;
        public long end;
        public String word;
        public Lang lang = Lang.Chinese;
    }

    public List<Tone> tones;

    public LrcEntryData(Tone tone) {
        this.tones = new ArrayList<>();
        this.tones.add(tone);
    }

    public LrcEntryData(List<Tone> tones) {
        this.tones = tones;
    }

    public long getStartTime() {
        if (tones == null || tones.size() <= 0) {
            return 0;
        }

        Tone first = tones.get(0);
        return first.begin;
    }
}
