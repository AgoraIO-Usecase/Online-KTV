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

package io.agora.lrcview;

import android.text.TextUtils;
import android.text.format.DateUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agora.lrcview.bean.IEntry;
import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryDefault;

/**
 * 工具类
 */
class LrcLoadDefaultUtils {
    private static final Pattern PATTERN_LINE = Pattern.compile("((\\[\\d{2}:\\d{2}\\.\\d{2,3}\\])+)(.+)");
    private static final Pattern PATTERN_TIME = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]");

    /**
     * 从文件解析歌词
     */
    public static LrcData parseLrc(File lrcFile) {
        if (lrcFile == null || !lrcFile.exists()) {
            return null;
        }

        LrcData mLrcData = new LrcData(IEntry.Type.Default);

        List<IEntry> entryList = new ArrayList<>();
        mLrcData.setEntrys(entryList);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lrcFile), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                List<LrcEntryDefault> list = parseLine(line);
                if (list != null && !list.isEmpty()) {
                    entryList.addAll(list);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mLrcData;
    }

    /**
     * 解析一行歌词
     */
    private static List<LrcEntryDefault> parseLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return null;
        }

        line = line.trim();
        // [00:17.65]让我掉下眼泪的
        Matcher lineMatcher = PATTERN_LINE.matcher(line);
        if (!lineMatcher.matches()) {
            return null;
        }

        String times = lineMatcher.group(1);
        if (times == null) {
            return null;
        }

        String text = lineMatcher.group(3);
        List<LrcEntryDefault> entryList = new ArrayList<>();

        // [00:17.65]
        Matcher timeMatcher = PATTERN_TIME.matcher(times);
        while (timeMatcher.find()) {
            long min = Long.parseLong(timeMatcher.group(1));
            long sec = Long.parseLong(timeMatcher.group(2));
            String milString = timeMatcher.group(3);
            long mil = Long.parseLong(milString);
            // 如果毫秒是两位数，需要乘以10
            if (milString.length() == 2) {
                mil = mil * 10;
            }
            long time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil;
            entryList.add(new LrcEntryDefault(time, text));
        }
        return entryList;
    }
}
