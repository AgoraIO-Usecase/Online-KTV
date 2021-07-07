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

/**
 * 一行歌词实体
 */
public class LrcEntryDefault implements IEntry {
    public long time;
    public String text;

    public LrcEntryDefault(long time, String text) {
        this.time = time;
        this.text = text;
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
    public long getDuration() {
        return 0;
    }

    @Override
    public String getText() {
        return text;
    }
}
