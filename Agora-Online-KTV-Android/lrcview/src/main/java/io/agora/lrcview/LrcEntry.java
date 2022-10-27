package io.agora.lrcview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.agora.lrcview.bean.LrcEntryData;

/**
 * 处理每一行歌词
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
class LrcEntry {
    private static final String TAG = "LrcEntryData";

    private StaticLayout mLayoutBG; // 背景文字
    private StaticLayout mLayoutFG; // 前排高亮文字

    private Rect[] drawRects; // 控制进度

    private Rect[] textRectTotalWords; // 每一段歌词
    private Rect[] textRectDisplayLines; // 每一行显示的歌词

    private LrcEntryData mEntry; // 数据源

    public enum Gravity {
        CENTER(0), LEFT(1), RIGHT(2);

        int value = 0;

        Gravity(int value) {
            this.value = value;
        }

        static Gravity parse(int value) {
            if (value == 0) {
                return CENTER;
            } else if (value == 1) {
                return LEFT;
            } else if (value == 2) {
                return RIGHT;
            } else {
                return CENTER;
            }
        }
    }

    public LrcEntry(LrcEntryData mEntry, @NonNull TextPaint mTextPaintBG, int width, Gravity gravity) {
        this.mEntry = mEntry;
        this.init(null, mTextPaintBG, width, gravity);
    }

    public LrcEntry(LrcEntryData mEntry, @Nullable TextPaint mTextPaintFG, @NonNull TextPaint mTextPaintBG, int width, Gravity gravity) {
        this.mEntry = mEntry;
        this.init(mTextPaintFG, mTextPaintBG, width, gravity);
    }

    private void init(@Nullable TextPaint mTextPaintFG, @NonNull TextPaint mTextPaintBG, int width, Gravity gravity) {
        Layout.Alignment align;
        switch (gravity) {
            case LEFT:
                align = Layout.Alignment.ALIGN_NORMAL;
                break;
            default:
            case CENTER:
                align = Layout.Alignment.ALIGN_CENTER;
                break;
            case RIGHT:
                align = Layout.Alignment.ALIGN_OPPOSITE;
                break;
        }

        StringBuilder sb = new StringBuilder();
        List<LrcEntryData.Tone> tones = mEntry.tones;
        textRectTotalWords = new Rect[tones.size()];
        String text;
        for (int i = 0; i < tones.size(); i++) {
            LrcEntryData.Tone tone = tones.get(i);
            Rect rectTotal = new Rect();
            textRectTotalWords[i] = rectTotal;
            String s = tone.word;
            if (tone.lang != LrcEntryData.Lang.Chinese) {
                s = s + " ";
            }
            sb.append(s);

            text = sb.toString();
            mTextPaintBG.getTextBounds(text, 0, text.length(), rectTotal);
        }

        text = sb.toString();
        if (mTextPaintFG != null) {
            mLayoutFG = new StaticLayout(text, mTextPaintFG, width, align, 1f, 0f, false);
        }
        mLayoutBG = new StaticLayout(text, mTextPaintBG, width, align, 1f, 0f, false);

        int totalLine = mLayoutBG.getLineCount();
        textRectDisplayLines = new Rect[totalLine];
        drawRects = new Rect[totalLine];
        for (int i = 0; i < totalLine; i++) {
            Rect mRect = new Rect();
            mLayoutBG.getLineBounds(i, mRect);
            mRect.left = (int) mLayoutBG.getLineLeft(i);
            mRect.right = (int) mLayoutBG.getLineRight(i);

            textRectDisplayLines[i] = mRect;
            drawRects[i] = new Rect(mRect);
        }
    }

    int getHeight() {
        if (mLayoutBG == null) {
            return 0;
        }
        return mLayoutBG.getHeight();
    }

    void draw(Canvas canvas) {
        mLayoutBG.draw(canvas);
    }

    void drawFG(Canvas canvas) {
        mLayoutFG.draw(canvas);
    }

    Rect[] getDrawRectByTime(long time) {
        int doneLen = 0;
        float curLen = 0f;

        List<LrcEntryData.Tone> tones = mEntry.tones;
        for (int i = 0; i < tones.size(); i++) {
            LrcEntryData.Tone tone = tones.get(i);
            if (time >= tone.end) {
                if (mLayoutFG.getLineCount() == 1) {
                    if (i == tones.size() - 1) {
                        doneLen = textRectDisplayLines[0].width();
                    } else {
                        doneLen = textRectTotalWords[i].width();
                    }
                } else {
                    doneLen = textRectTotalWords[i].width();
                }
            } else {
                int wordLen = 0;
                if (i == 0) {
                    wordLen = textRectTotalWords[i].width();
                } else {
                    wordLen = textRectTotalWords[i].width() - textRectTotalWords[i - 1].width();
                }

                float percent = (time - tone.begin) / (float) (tone.end - tone.begin);

                curLen = wordLen * (percent > 0 ? percent : 0);
                break;
            }
        }

        int showLen = (int) (doneLen + curLen);
        for (int i = 0; i < mLayoutFG.getLineCount(); i++) {
            int curLineWidth = textRectDisplayLines[i].width();
            drawRects[i].left = textRectDisplayLines[i].left;
            drawRects[i].right = textRectDisplayLines[i].right;
            if (curLineWidth > showLen) {
                drawRects[i].right = drawRects[i].left + showLen;
                showLen = 0;
            } else {
                showLen -= curLineWidth;
            }
        }

        return drawRects;
    }
}
