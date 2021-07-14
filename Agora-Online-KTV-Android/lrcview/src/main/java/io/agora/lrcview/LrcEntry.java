package io.agora.lrcview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import io.agora.lrcview.bean.IEntry;

/**
 * 处理每一行歌词
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/6
 */
public class LrcEntry {
    private static final String TAG = "LrcEntry";
    private StaticLayout fgLayout1;
    private StaticLayout bgLayout1;

    private Rect[] textRects1;
    private Rect[] drawRects;
    private Rect[] textRects;

    private int text1Len = 0;

    private IEntry mIEntry;

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

    public LrcEntry(IEntry mIEntry) {
        this.mIEntry = mIEntry;
    }

    void init(TextPaint fgPaint, TextPaint bgPaint, int width, Gravity gravity) {
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

        String[] texts = mIEntry.getTexts();
        textRects = new Rect[texts.length];
        for (int i = 0; i < texts.length; i++) {
            Rect rect = new Rect();
            textRects[i] = rect;
            String s = texts[i];
            fgPaint.getTextBounds(s, 0, s.length(), rect);
        }

        String text = mIEntry.getText();
        fgLayout1 = new StaticLayout(text, fgPaint, width, align, 1f, 0f, false);
        bgLayout1 = new StaticLayout(text, bgPaint, width, align, 1f, 0f, false);

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
        }

        drawRects = new Rect[totalLine];

        for (int i = 0; i < fgLayout1.getLineCount(); i++) {
            drawRects[i] = new Rect(textRects1[i]);
        }
    }

    int getHeight() {
        if (fgLayout1 == null) {
            return 0;
        }
        return fgLayout1.getHeight();
    }

    void drawFg(Canvas canvas) {
        canvas.save();
        fgLayout1.draw(canvas);
        canvas.restore();
    }

    void drawBg(Canvas canvas) {
        canvas.save();
        bgLayout1.draw(canvas);
        canvas.restore();
    }

    private float prePct = 0;

    Rect[] getDrawRectByTime(long time) {
        float pct = mIEntry.getOffset(time);
        if (pct < prePct) {
            pct = prePct;
        } else {
            prePct = pct;
        }

        int showLen1 = (int) (text1Len * pct);

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
        }
        return drawRects;
    }
}
