/*
 * Copyright (C) 2017 wangchenyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.agora.lrcview.bean;

import java.util.List;

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

    @Override
    public long getDuration() {
        Tone first = tones.get(0);
        Tone last = tones.get(tones.size() - 1);
        return (long) ((last.end - first.begin) * 1000L);
    }

    private String text;

    @Override
    public String getText() {
        if (text == null) {
            StringBuilder sb = new StringBuilder();
            for (Tone tone : tones) {
                sb.append(tone.word);
                if (!"1".equals(tone.lang)) {
                    sb.append(" ");
                }
            }
            text = sb.toString();
        }
        return text;
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
