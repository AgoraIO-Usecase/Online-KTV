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
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.agora.lrcview.bean.IEntry;
import io.agora.lrcview.bean.LrcData;

@SuppressLint("StaticFieldLeak")
public class LrcView extends View {
    private static final String TAG = "LrcView";

    private final List<IEntry> entrys = new ArrayList<>();
    private final TextPaint mLrcPaint = new TextPaint();
    private final TextPaint mBgLrcPaint = new TextPaint();
    private int mNormalTextColor;
    private float mNormalTextSize;
    private int mCurrentTextColor;
    private float mCurrentTextSize;
    private float mDividerHeight;
    private String mDefaultLabel;
    private int mCurrentLine = 0;
    /**
     * 歌词显示位置，靠左/居中/靠右
     */
    private LrcEntry.Gravity mTextGravity;

    private boolean mNewLine = true;
    private Bitmap mFgText1 = null;
    private Bitmap mBgText1 = null;
    private Canvas mFgTextCanvas1 = null;
    private Canvas mBgTextCanvas1 = null;
    private final Rect mTextBmpRect = new Rect();
    private final Rect mTextRenderRect = new Rect();

    private long mCurrentTime = 0;
    private long mTotalDuration = 0;

    public LrcView(Context context) {
        this(context, null);
    }

    public LrcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.LrcView);
        mCurrentTextSize = ta.getDimension(R.styleable.LrcView_lrcTextSize, getResources().getDimension(R.dimen.lrc_text_size));
        mNormalTextSize = ta.getDimension(R.styleable.LrcView_lrcNormalTextSize, getResources().getDimension(R.dimen.lrc_text_size));
        if (mNormalTextSize == 0) {
            mNormalTextSize = mCurrentTextSize;
        }

        mDividerHeight = ta.getDimension(R.styleable.LrcView_lrcDividerHeight, getResources().getDimension(R.dimen.lrc_divider_height));
        mNormalTextColor = ta.getColor(R.styleable.LrcView_lrcNormalTextColor, getResources().getColor(R.color.lrc_normal_text_color));
        mCurrentTextColor = ta.getColor(R.styleable.LrcView_lrcCurrentTextColor, getResources().getColor(R.color.lrc_current_text_color));
        mDefaultLabel = ta.getString(R.styleable.LrcView_lrcLabel);
        mDefaultLabel = TextUtils.isEmpty(mDefaultLabel) ? getContext().getString(R.string.lrc_label) : mDefaultLabel;
        int lrcTextGravity = ta.getInteger(R.styleable.LrcView_lrcTextGravity, 0);
        mTextGravity = LrcEntry.Gravity.parse(lrcTextGravity);

        ta.recycle();

        mLrcPaint.setTextSize(mCurrentTextSize);
        mLrcPaint.setColor(mCurrentTextColor);
        mLrcPaint.setAntiAlias(true);
        mLrcPaint.setTextAlign(Paint.Align.LEFT);

        mBgLrcPaint.setColor(mNormalTextColor);
        mBgLrcPaint.setAntiAlias(true);
        mBgLrcPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setTotalDuration(long d) {
        mTotalDuration = d;
    }

    /**
     * 设置非当前行歌词字体颜色
     */
    public void setNormalColor(int normalColor) {
        mNormalTextColor = normalColor;
        invalidate();
    }

    /**
     * 普通歌词文本字体大小
     */
    public void setNormalTextSize(float size) {
        mNormalTextSize = size;
    }

    /**
     * 当前歌词文本字体大小
     */
    public void setCurrentTextSize(float size) {
        mCurrentTextSize = size;
    }

    /**
     * 设置当前行歌词的字体颜色
     */
    public void setCurrentColor(int currentColor) {
        mCurrentTextColor = currentColor;
        invalidate();
    }

    /**
     * 设置歌词为空时屏幕中央显示的文字，如“暂无歌词”
     */
    public void setLabel(String label) {
        mDefaultLabel = label;
        invalidate();
    }

    /**
     * 加载歌词文件
     *
     * @param lrcFile 歌词文件
     */
    public void loadLrc(File lrcFile) {
        reset();

        LrcLoadUtils.execute(new Runnable() {
            @Override
            public void run() {
                LrcData data = LrcLoadUtils.parse(lrcFile);
                onLrcLoaded(data);
            }
        });
    }

    /**
     * 歌词是否有效
     *
     * @return true，如果歌词有效，否则false
     */
    public boolean hasLrc() {
        return !entrys.isEmpty();
    }

    /**
     * 刷新歌词
     *
     * @param time 当前播放时间
     */
    public void updateTime(long time) {
        if (!hasLrc()) {
            return;
        }

        if (isLrcLoadDone == false) {
            return;
        }

        mCurrentTime = time;

        int line = findShowLine(time);
        if (line != mCurrentLine) {
            mNewLine = true;
            mCurrentLine = line;
        }
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            //TODO: new bitmap and cavas
            int w = right - left - getPaddingStart() - getPaddingEnd();
            int h = bottom - top - getPaddingTop() - getPaddingBottom();

            if (mFgText1 == null) {
                mFgText1 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                mFgTextCanvas1 = new Canvas(mFgText1);
            }

            if (mBgText1 == null) {
                mBgText1 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                mBgTextCanvas1 = new Canvas(mBgText1);
            }
        }
    }

    private LrcEntry curLrcEntry;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 无歌词文件
        if (!hasLrc()) {
            mLrcPaint.setTextSize(mCurrentTextSize);
            mLrcPaint.setColor(mCurrentTextColor);

            int width = getLrcWidth();
            int height = getLrcHeight();
            @SuppressLint("DrawAllocation")
            StaticLayout staticLayout = new StaticLayout(
                    mDefaultLabel,
                    mLrcPaint,
                    width,
                    Layout.Alignment.ALIGN_CENTER,
                    1f,
                    0f,
                    false);

            canvas.save();
            float y = getPaddingTop() + (height - staticLayout.getHeight()) / 2F;
            canvas.translate(0, y);
            staticLayout.draw(canvas);
            canvas.restore();
            return;
        }

        IEntry cur = entrys.get(mCurrentLine);
        if (mNewLine) {
            curLrcEntry = cur.createLRCEntry();

            mLrcPaint.setTextSize(mCurrentTextSize);
            mBgLrcPaint.setTextSize(mCurrentTextSize);
            curLrcEntry.init(mLrcPaint, mBgLrcPaint, getLrcWidth(), mTextGravity);

            // clear bitmap
            mFgText1.eraseColor(0);
            mBgText1.eraseColor(0);

            //TODO: draw text on the bitmap
            if (mCurrentLine < 0 || mCurrentLine >= entrys.size()) {
                return;
            }

            mFgTextCanvas1.save();
            mBgTextCanvas1.save();

            float curPointY = 0;

            //draw current
            float y = (getLrcHeight() - curLrcEntry.getHeight()) / 2F;
            mFgTextCanvas1.translate(0, y);
            curLrcEntry.drawFg(mFgTextCanvas1);

            mBgTextCanvas1.translate(0, y);
            curLrcEntry.drawBg(mBgTextCanvas1);
            curPointY = y;

            IEntry line = null;
            LrcEntry mLrcEntry = null;
            //draw top
            mLrcPaint.setTextSize(mNormalTextSize);
            mBgLrcPaint.setTextSize(mNormalTextSize);
            for (int i = mCurrentLine - 1; i >= 0; i--) {
                line = entrys.get(i);
                mLrcEntry = line.createLRCEntry();
                mLrcEntry.init(mLrcPaint, mBgLrcPaint, getLrcWidth(), mTextGravity);

                if (curPointY - mDividerHeight - mLrcEntry.getHeight() < 0)
                    break;

                y = mDividerHeight + mLrcEntry.getHeight();
                mBgTextCanvas1.translate(0, -y);
                mLrcEntry.drawBg(mBgTextCanvas1);
                curPointY = curPointY - y;
            }

            //draw bottom
            y = (getLrcHeight() + curLrcEntry.getHeight()) / 2F - curPointY + mDividerHeight;
            mBgTextCanvas1.translate(0, y);
            curPointY = curPointY + y;

            for (int i = mCurrentLine + 1; i < entrys.size(); i++) {
                line = entrys.get(i);
                mLrcEntry = line.createLRCEntry();
                mLrcEntry.init(mLrcPaint, mBgLrcPaint, getLrcWidth(), mTextGravity);

                if (curPointY + mLrcEntry.getHeight() > getLrcHeight())
                    break;

                mLrcEntry.drawBg(mBgTextCanvas1);
                y = mLrcEntry.getHeight() + mDividerHeight;
                mBgTextCanvas1.translate(0, y);
                curPointY = curPointY + y;
            }
            mFgTextCanvas1.restore();
            mBgTextCanvas1.restore();

            mNewLine = false;
        }

        //TODO: draw bg text to the canvas
        mTextBmpRect.left = 0;
        mTextBmpRect.top = 0;
        mTextBmpRect.right = getLrcWidth();
        mTextBmpRect.bottom = getLrcHeight();

        mTextRenderRect.left = getPaddingStart();
        mTextRenderRect.top = getPaddingTop();
        mTextRenderRect.right = getPaddingStart() + getLrcWidth();
        mTextRenderRect.bottom = getPaddingTop() + getLrcHeight();

        canvas.drawBitmap(mBgText1, mTextBmpRect, mTextRenderRect, null);

        if (cur.getDuration() == 0) {
            canvas.drawBitmap(mFgText1, mTextBmpRect, mTextRenderRect, null);
        } else {
            //TODO: get fg text draw rect by current timestamp
            Rect[] drawRects = curLrcEntry.getDrawRectByTime(mCurrentTime);

            //TODO: draw fg text to the canvas
            for (Rect dr : drawRects) {
                if (dr.left == dr.right)
                    continue;

                mTextBmpRect.right = dr.right;
                mTextRenderRect.right = getPaddingStart() + dr.right;
                canvas.drawBitmap(mFgText1, mTextBmpRect, mTextRenderRect, null);
            }
        }
    }

    private volatile boolean isLrcLoadDone = false;

    private void onLrcLoaded(LrcData data) {
        if (data != null) {
            List<IEntry> entryList = data.getEntrys();
            if (entryList != null && !entryList.isEmpty()) {
                entrys.addAll(entryList);
            }
        }

        isLrcLoadDone = true;
        postInvalidate();
    }

    public void reset() {
        entrys.clear();
        mCurrentLine = 0;
        mNewLine = true;
        mCurrentTime = 0;
        isLrcLoadDone = false;
        invalidate();
    }

    /**
     * 二分法查找当前时间应该显示的行数（最后一个 <= time 的行数）
     */
    private int findShowLine(long time) {
        int left = 0;
        int right = entrys.size();
        while (left <= right) {
            int middle = (left + right) / 2;
            long middleTime = entrys.get(middle).getTime();

            if (time < middleTime) {
                right = middle - 1;
            } else {
                if (middle + 1 >= entrys.size() || time < entrys.get(middle + 1).getTime()) {
                    return middle;
                }

                left = middle + 1;
            }
        }

        return 0;
    }

    private int getLrcWidth() {
        return getWidth() - getPaddingStart() - getPaddingEnd();
    }

    private int getLrcHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }
}
