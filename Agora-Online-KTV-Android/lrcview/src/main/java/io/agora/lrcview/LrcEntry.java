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

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;


/**
 * 一行歌词实体
 */
class LrcEntry implements Comparable<LrcEntry> {
    private static final String TAG = "LrcEntry";
    private long time;
    private long duration;
    private String text;
    private String secondText;
    private StaticLayout fgLayout1;
    private StaticLayout fgLayout2;
    private StaticLayout bgLayout1;
    private StaticLayout bgLayout2;

    private Rect[] textRects1;
    // private Rect[] drawRects1;
    private Rect[] textRects2;
    // private Rect[] drawRects2;
    private Rect[] drawRects;

    private int text1Len = 0;
    private int text2Len = 0;

    /**
     * 歌词距离视图顶部的距离
     */
    private float offset = Float.MIN_VALUE;
    public static final int GRAVITY_CENTER = 0;
    public static final int GRAVITY_LEFT = 1;
    public static final int GRAVITY_RIGHT = 2;

    LrcEntry(long time, String text) {
        this.time = time;
        this.duration = 5000;
        this.text = text;
    }

    LrcEntry(long time, String text, String secondText) {
        this.time = time;
        this.duration = 5000;
        this.text = text;
        this.secondText = secondText;
    }

    void init(TextPaint fgPaint, TextPaint bgPaint, int width, int gravity) {
        Layout.Alignment align;
        switch (gravity) {
            case GRAVITY_LEFT:
                align = Layout.Alignment.ALIGN_NORMAL;
                break;

            default:
            case GRAVITY_CENTER:
                align = Layout.Alignment.ALIGN_CENTER;
                break;

            case GRAVITY_RIGHT:
                align = Layout.Alignment.ALIGN_OPPOSITE;
                break;
        }
        fgLayout1 = new StaticLayout(text, fgPaint, width, align, 1f, 0f, false);
        if (!TextUtils.isEmpty(secondText)) {
            fgLayout2 = new StaticLayout(secondText, fgPaint, width, align, 1f, 0f, false);
        }
        bgLayout1 = new StaticLayout(text, bgPaint, width, align, 1f, 0f, false);
        if (!TextUtils.isEmpty(secondText)) {
            bgLayout2 = new StaticLayout(secondText, bgPaint, width, align, 1f, 0f, false);
        }

        int totalLine = fgLayout1.getLineCount();
        text1Len = 0;
        textRects1 = new Rect[fgLayout1.getLineCount()];
        for (int i = 0; i < fgLayout1.getLineCount(); i++) {
            Rect newLine = new Rect();
            textRects1[i] = newLine;
            fgLayout1.getLineBounds(i, textRects1[i]);
            newLine.left = (int) fgLayout1.getLineLeft(i);
            newLine.right = (int) fgLayout1.getLineRight(i);
            text1Len += newLine.right - newLine.left;
            Log.i(TAG, String.format("init: line bounds: (%d, %d, %d, %d)", newLine.left, newLine.top, newLine.right, newLine.bottom));
        }
        if (fgLayout2 != null) {
            totalLine += fgLayout2.getLineCount();
            text2Len = 0;
            textRects2 = new Rect[fgLayout2.getLineCount()];
            for (int i = 0; i < fgLayout2.getLineCount(); i++) {
                Rect newLine = new Rect();
                textRects2[i] = newLine;
                fgLayout2.getLineBounds(i, textRects2[i]);
                newLine.left = (int) fgLayout2.getLineLeft(i);
                newLine.right = (int) fgLayout2.getLineRight(i);
                text2Len += newLine.right - newLine.left;
                Log.i(TAG, String.format("init: line bounds: (%d, %d, %d, %d)", newLine.left, newLine.top, newLine.right, newLine.bottom));
            }
        }

        /* drawRects1 = new Rect[fgLayout1.getLineCount()];
        if (fgLayout1.getLineCount() >= 0)
            System.arraycopy(textRects1, 0, drawRects1, 0, fgLayout1.getLineCount());
        if (fgLayout2 != null) {
            drawRects2 = new Rect[fgLayout2.getLineCount()];
            if (fgLayout2.getLineCount() >= 0)
                System.arraycopy(textRects2, 0, drawRects2, 0, fgLayout2.getLineCount());
        } */

        drawRects = new Rect[totalLine];

        for (int i = 0; i < fgLayout1.getLineCount(); i++) {
            drawRects[i] = new Rect(textRects1[i]);
        }
        if (fgLayout2 != null) {
            int text1Height = fgLayout1.getHeight();
            for (int i = 0, j = fgLayout1.getLineCount(); i < fgLayout2.getLineCount(); i++, j++) {
                drawRects[j] = new Rect(textRects2[i]);
                drawRects[j].top += text1Height;
                drawRects[j].bottom += text1Height;
            }
        }
        offset = Float.MIN_VALUE;
    }

    long getTime() {
        return time;
    }

    void setDuration(long d) {
        duration = d;
    }

    int getHeight() {
        if (fgLayout1 == null) {
            return 0;
        }
        int height = fgLayout1.getHeight();
        if (fgLayout2 != null) {
            height += fgLayout2.getHeight();
        }
        return height;
    }

    void drawFg(Canvas canvas) {
        canvas.save();
        fgLayout1.draw(canvas);
        if (fgLayout2 != null) {
            canvas.translate(0, fgLayout1.getHeight());
            fgLayout2.draw(canvas);
        }
        canvas.restore();
    }

    void drawBg(Canvas canvas) {
        canvas.save();
        bgLayout1.draw(canvas);
        if (bgLayout2 != null) {
            canvas.translate(0, bgLayout1.getHeight());
            bgLayout2.draw(canvas);
        }
        canvas.restore();
    }

    @SuppressLint("DefaultLocale")
    Rect[] getDrawRectByTime(long time) {
        StringBuilder logStr = new StringBuilder("time: " + time);
        float pct = ((float) (time - this.time)) / ((float) this.duration);
        if (pct < 0)
            pct = 0;
        if (pct > 1)
            pct = 1;
        int showLen1 = (int) (text1Len * pct);
        int showLen2 = (int) (text2Len * pct);
        logStr.append(", lrcTime: ").append(this.time).append(", duration: ").append(this.duration);
        logStr.append("\npct: ").append(pct).append(", showLen1: ").append(showLen1).append(", showLen2: ").append(showLen2);

        for (int i = 0; i < fgLayout1.getLineCount(); i++) {
            int curLineWidth = textRects1[i].right - textRects1[i].left;
            drawRects[i].left = textRects1[i].left;
            drawRects[i].right = textRects1[i].right;
            if (curLineWidth > showLen1) {
                drawRects[i].right = drawRects[i].left + showLen1;
                showLen1 = 0;
            } else {
                showLen1 -= curLineWidth;
            }
            logStr.append("\ndrawRect").append(i).append(String.format(": (%d, %d, %d, %d)", drawRects[i].left, drawRects[i].top, drawRects[i].right, drawRects[i].bottom));
        }

        if (fgLayout2 != null) {
            for (int i = 0, j = fgLayout1.getLineCount(); i < fgLayout2.getLineCount(); i++, j++) {
                int curLineWidth = textRects2[i].right - textRects2[i].left;
                drawRects[j].left = textRects2[i].left;
                drawRects[j].right = textRects2[i].right;
                if (curLineWidth > showLen2) {
                    drawRects[j].right = drawRects[j].left + showLen2;
                    showLen2 = 0;
                } else {
                    showLen2 -= curLineWidth;
                }
                logStr.append("\ndrawRect").append(j).append(String.format(": (%d, %d, %d, %d)", drawRects[j].left, drawRects[j].top, drawRects[j].right, drawRects[j].bottom));
            }
        }
        Log.i(TAG, "getDrawRectByTime: " + logStr.toString());
        return drawRects;
    }

    public float getOffset() {
        return offset;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    String getText() {
        return text;
    }


    void setSecondText(String secondText) {
        this.secondText = secondText;
    }

    private String getShowText() {
        if (!TextUtils.isEmpty(secondText)) {
            return text + "\n" + secondText;
        } else {
            return text;
        }
    }

    @Override
    public int compareTo(LrcEntry entry) {
        if (entry == null) {
            return -1;
        }
        return (int) (time - entry.getTime());
    }
}
