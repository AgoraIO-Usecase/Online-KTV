package io.agora.lrcview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryData;

/**
 * 音调View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/08/04
 */
public class PitchView extends View {

    private static final int START_PERCENT = 30;

    private static volatile LrcData lrcData;

    private float widthPerSecond = 0.1F;//1ms对应像素px

    private int itemHeight = 10;//每一项高度px
    private int itemSpace = 4;//间距px

    private int pitchMax = 100;//最大值
    private int pitchMin = 50;//最小值

    private final Paint mPaint = new Paint();
    private int mNormalTextColor;
    private int mDoneTextColor;

    private float dotPointX = 0F;//亮点坐标

    public PitchView(Context context) {
        super(context);
        init(null);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.PitchView);
        mNormalTextColor = ta.getColor(R.styleable.PitchView_pitchNormalTextColor, getResources().getColor(R.color.lrc_normal_text_color));
        mDoneTextColor = ta.getColor(R.styleable.PitchView_pitchDoneTextColor, getResources().getColor(R.color.lrc_current_text_color));
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            int w = right - left;
            int h = bottom - top;
            dotPointX = w * START_PERCENT / 100F;

            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawStartLine(canvas);
        drawItems(canvas);
    }

    private void drawStartLine(Canvas canvas) {
        mPaint.setColor(Color.WHITE);
        canvas.drawLine(dotPointX, 0, dotPointX + 2, getHeight(), mPaint);
    }

    private void drawItems(Canvas canvas) {
        mPaint.setColor(mNormalTextColor);

        if (lrcData == null || lrcData.entrys == null || lrcData.entrys.isEmpty()) {
            return;
        }

        List<LrcEntryData> entrys = lrcData.entrys;
        float currentPX = this.mCurrentTime * widthPerSecond;
        float x = dotPointX - currentPX;
        float y = 0;
        float widthTone = 0;
        float mItemHeight = getHeight() / (float) (pitchMax - pitchMin);//高度
        long preEndTIme = 0;
        for (int i = 0; i < entrys.size(); i++) {
            LrcEntryData entry = lrcData.entrys.get(i);
            List<LrcEntryData.Tone> tones = entry.tones;

            long startTime = entry.getStartTime();
            float emptyPX = widthPerSecond * (startTime - preEndTIme);
            x = x + emptyPX;

            if (x >= getWidth()) {
                break;
            }

            preEndTIme = tones.get(tones.size() - 1).end;
            for (LrcEntryData.Tone tone : tones) {
                widthTone = widthPerSecond * tone.getDuration();
                float endX = x + widthTone;
                if (endX <= 0) {
                    x = endX;
                    continue;
                }

                if (x >= getWidth()) {
                    x = endX;
                    break;
                }

                y = (pitchMax - tone.pitch) * mItemHeight;
                RectF r = new RectF(x, y, endX, y + itemHeight);
                canvas.drawRect(r, mPaint);

                x = endX;
            }
        }
    }

    /**
     * 设置歌词信息
     *
     * @param data 歌词信息对象
     */
    public void setLrcData(LrcData data) {
        lrcData = data;
        invalidate();
    }

    private long mCurrentTime = 0;

    /**
     * 更新进度，单位毫秒
     *
     * @param time 当前播放时间，毫秒
     */
    public void updateTime(long time) {
        if (lrcData == null) {
            return;
        }

        this.mCurrentTime = time;

        invalidate();
    }

    /**
     * 重置内部状态，清空已经加载的歌词
     */
    public void reset() {
        lrcData = null;
        mCurrentTime = 0;

        invalidate();
    }
}
