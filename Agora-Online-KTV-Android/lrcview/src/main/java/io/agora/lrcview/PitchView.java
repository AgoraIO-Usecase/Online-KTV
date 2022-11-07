package io.agora.lrcview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.plattysoft.leonids.ParticleSystem;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryData;

/**
 * 音调 View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/08/04
 */
public class PitchView extends View {

    private static final boolean DEBUG = false;

    private static final float START_PERCENT = 0.4F;

    private static volatile LrcData lrcData;
    private Handler mHandler;

    private float movedPixelsPerMs = 0.2F; // 1ms 对应像素 px

    private int pitchStickHeight; // 每一项高度 px
    private int pitchStickSpace = 4; // 间距 px

    private int pitchMax = 0; // 最大值
    private int pitchMin = 100; // 最小值
    private int totalPitch = 0;

    // 完成 PitchView.OnActionListener#onOriginalPitch 的需求
    // 当前 Pitch 所在的字的开始时间
    private long currentPitchStartTime = -1;
    // 当前 Pitch 所在的字的结束时间
    private long currentPitchEndTime = -1;
    // 当前 Pitch 所在的句的结束时间
    private long currentEntryEndTime = -1;
    // 当前在打分的所在句的结束时间
    private long lrcEndTime = 0;
    // 当前 时间的 Pitch，不断变化
    private float mCurrentPitch = 0f;

    // 音调指示器的半径
    private int mLocalPitchIndicatorRadius;
    // 每句最高分
    private int scorePerSentence = 100;
    // 初始分数
    private float mInitialScore;
    // 每句歌词分数
    public LinkedHashMap<Long, Double> everyPitchList = new LinkedHashMap<>();
    // 累计分数
    public float cumulatedScore;
    // 歌曲总分数
    public float totalScore;
    // 分数阈值 大于此值计分 小于不计分
    public final float scoreCountLine = 0.4f;

    private final Paint mLocalPitchIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mLocalPitchIndicatorColor;

    private final Paint mLinearGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient linearGradient;

    private final Paint mPitchStickLinearGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mOriginPitchStickColor;
    private final Paint mHighlightPitchStickLinearGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mHighlightPitchStickColor;

    private final Paint mTailAnimationLinearGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient mTailAnimationLinearGradient;

    private float dotPointX = 0F; // 亮点坐标

    private ParticleSystem mParticleSystem;

    // 音调及分数回调
    public OnSingScoreListener onActionListener;

    //<editor-fold desc="Init Related">
    public PitchView(Context context) {
        this(context, null);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
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
        mLocalPitchIndicatorRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        if (attrs == null) {
            return;
        }
        this.mHandler = new Handler(Looper.myLooper());
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.PitchView);
        mLocalPitchIndicatorColor = ta.getColor(R.styleable.PitchView_pitchIndicatorColor, getResources().getColor(R.color.local_pitch_indicator_color));
        mInitialScore = ta.getFloat(R.styleable.PitchView_pitchInitialScore, 50f);
        ta.recycle();

        mOriginPitchStickColor = getResources().getColor(R.color.lrc_normal_text_color);
        mHighlightPitchStickColor = getResources().getColor(R.color.pitch_stick_highlight_color);

        int startColor = getResources().getColor(R.color.pitch_start);
        int endColor = getResources().getColor(R.color.pitch_end);
        linearGradient = new LinearGradient(dotPointX, 0, 0, 0, startColor, endColor, Shader.TileMode.CLAMP);

        mTailAnimationLinearGradient = new LinearGradient(dotPointX, 0, dotPointX - 12, 0, startColor, Color.YELLOW, Shader.TileMode.CLAMP);

        pitchStickHeight = dip2px(getContext(), 6);
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
            dotPointX = w * START_PERCENT;

            int startColor = getResources().getColor(R.color.pitch_start);
            int endColor = getResources().getColor(R.color.pitch_end);
            linearGradient = new LinearGradient(dotPointX, 0, 0, 0, startColor, endColor, Shader.TileMode.CLAMP);

            mTailAnimationLinearGradient = new LinearGradient(dotPointX, 0, dotPointX - 12, 0, startColor, Color.YELLOW, Shader.TileMode.CLAMP);

            invalidate();

            mHandler.postDelayed(() -> {
                // Create a particle system and start emiting
                mParticleSystem = new ParticleSystem((ViewGroup) this.getParent(), 8, getResources().getDrawable(R.drawable.pitch_indicator), 400);

                // It works with an emision range
                int[] location = new int[2];
                this.getLocationInWindow(location);
                mParticleSystem.setRotationSpeedRange(90, 180).setScaleRange(0.7f, 1.3f)
                        .setSpeedModuleAndAngleRange(0.03f, 0.12f, 120, 240)
                        .setFadeOut(200, new AccelerateInterpolator());
            }, 1000);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawStartLine(canvas);
        drawPitchSticks(canvas);
        drawLocalPitchStick(canvas);
    }
    //</editor-fold>

    //<editor-fold desc="Draw Related">
    private void drawLocalPitchStick(Canvas canvas) {
        mLocalPitchIndicatorPaint.setShader(null);
        mLocalPitchIndicatorPaint.setColor(mLocalPitchIndicatorColor);

        canvas.drawLine(dotPointX, 0, dotPointX, getHeight(), mLocalPitchIndicatorPaint);

        float value = getYForPitchPivot();
        if (value >= 0) {
            if (mInHighlightStatus) {
                // Perform the tail animation if in highlight status
                mTailAnimationLinearGradientPaint.setShader(null);
                mTailAnimationLinearGradientPaint.setShader(mTailAnimationLinearGradient);
                mTailAnimationLinearGradientPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mTailAnimationLinearGradientPaint.setAntiAlias(true);

                Path path = new Path();
                path.moveTo(dotPointX, value - 6);
                path.lineTo(dotPointX, value + 6);
                path.lineTo(dotPointX - 100, value);
                path.close();
                canvas.drawPath(path, mTailAnimationLinearGradientPaint);
            }

            canvas.drawCircle(dotPointX, value, mLocalPitchIndicatorRadius, mLocalPitchIndicatorPaint);
        }
    }

    private float getYForPitchPivot() {
        float targetY = 0;
        if (this.mLocalPitch >= pitchMin && pitchMax != 0) { // Has value, not the default case
            float realPitchMax = pitchMax + 5;
            float realPitchMin = pitchMin - 5;
            float mItemHeightPerPitchLevel = getHeight() / (realPitchMax - realPitchMin);
            targetY = (realPitchMax - this.mLocalPitch) * mItemHeightPerPitchLevel;
        } else if (this.mLocalPitch < pitchMin) { // minimal local pitch
            targetY = getHeight();
        }

        if (targetY <= this.mLocalPitchIndicatorRadius) { // clamping it under the line
            targetY += this.mLocalPitchIndicatorRadius;
        }
        if (targetY >= getHeight() - this.mLocalPitchIndicatorRadius) { // clamping it above the line
            targetY -= this.mLocalPitchIndicatorRadius;
        }
        return targetY;
    }

    private void drawStartLine(Canvas canvas) {
        mLinearGradientPaint.setShader(null);
        mLinearGradientPaint.setShader(linearGradient);
        canvas.drawRect(0, 0, dotPointX, getHeight(), mLinearGradientPaint);
    }

    private void drawPitchSticks(Canvas canvas) {
        mPitchStickLinearGradientPaint.setShader(null);
        mPitchStickLinearGradientPaint.setColor(mOriginPitchStickColor);

        mHighlightPitchStickLinearGradientPaint.setShader(null);
        mHighlightPitchStickLinearGradientPaint.setColor(mHighlightPitchStickColor);

        if (lrcData == null || lrcData.entrys == null || lrcData.entrys.isEmpty()) {
            return;
        }

        float realPitchMax = pitchMax + 5;
        float realPitchMin = pitchMin - 5;

        List<LrcEntryData> entrys = lrcData.entrys;

        float y;
        float widthOfPitchStick;
        float mItemHeightPerPitchLevel = getHeight() / (realPitchMax - realPitchMin);

        long preEntryEndTime = 0; // Not used so far

        for (int i = 0; i < entrys.size(); i++) {
            LrcEntryData entry = lrcData.entrys.get(i);
            List<LrcEntryData.Tone> tones = entry.tones;
            if (tones == null || tones.isEmpty()) {
                return;
            }

            long startTime = entry.getStartTime();
            long durationOfCurrentEntry = entry.getEndTime() - startTime;

            if (this.mCurrentTime - startTime <= -(2 * durationOfCurrentEntry)) { // If still to early for current entry, we do not draw the sticks
                // If we show the sticks too late, they will appear suddenly in the central of screen, not start from the right side
                break;
            }

            if (i + 1 < entrys.size() && entry.getStartTime() < this.mCurrentTime) { // Has next entry
                // Get next entry
                // If start for next is far away than 2 seconds
                // stop the current animation now
                long nextEntryStartTime = lrcData.entrys.get(i + 1).getStartTime();
                if ((nextEntryStartTime - entry.getEndTime() >= 2 * 1000) && this.mCurrentTime > entry.getEndTime() && this.mCurrentTime < nextEntryStartTime) { // Two seconds after this entry stop
                    assureAnimationForPitchPivot(0); // Force stop the animation when there is a too long stop between two entrys
                    if (mTimestampForLastAnimationDecrease < 0 || this.mCurrentTime - mTimestampForLastAnimationDecrease > 4 * 1000) {
                        ObjectAnimator.ofFloat(PitchView.this, "mLocalPitch", PitchView.this.mLocalPitch, PitchView.this.mLocalPitch * 1 / 3, 0.0f).setDuration(600).start(); // Decrease the local pitch pivot
                        mTimestampForLastAnimationDecrease = this.mCurrentTime;
                    }
                }
            }

            float pixelsAwayFromPilot = (startTime - this.mCurrentTime) * movedPixelsPerMs; // For every time, we need to locate the new coordinate
            float x = dotPointX + pixelsAwayFromPilot;

            if (preEntryEndTime != 0) { // If has empty divider before
                // Not used so far
                int emptyDividerWidth = (int) (movedPixelsPerMs * (startTime - preEntryEndTime));
                x = x + emptyDividerWidth;
            }

            preEntryEndTime = entry.getEndTime();

            if (x + 2 * durationOfCurrentEntry * movedPixelsPerMs < 0) { // Already past for long time enough
                continue;
            }

            for (LrcEntryData.Tone tone : tones) {
                pixelsAwayFromPilot = (tone.begin - this.mCurrentTime) * movedPixelsPerMs; // For every time, we need to locate the new coordinate
                x = dotPointX + pixelsAwayFromPilot;
                widthOfPitchStick = movedPixelsPerMs * tone.getDuration();
                float endX = x + widthOfPitchStick;

                if (endX <= 0) {
                    continue;
                }

                if (x >= getWidth()) {
                    break;
                }

                y = (realPitchMax - tone.pitch) * mItemHeightPerPitchLevel;

                tone.highlight = mInHighlightStatus; // Mark this as highlight forever
                if (!DEBUG && tone.highlight) { // If DEBUG enabled, will not show highlight animation
                    if (x >= dotPointX) {
                        RectF rNormal = new RectF(x, y, endX, y + pitchStickHeight);
                        canvas.drawRoundRect(rNormal, 8, 8, mPitchStickLinearGradientPaint);
                    } else if (x < dotPointX && endX > dotPointX) {
                        RectF rNormalHalf = new RectF(dotPointX, y, endX, y + pitchStickHeight);
                        canvas.drawRoundRect(rNormalHalf, 8, 8, mPitchStickLinearGradientPaint);
                        RectF rHighlightHalf = new RectF(x, y, dotPointX, y + pitchStickHeight);
                        canvas.drawRoundRect(rHighlightHalf, 8, 8, mHighlightPitchStickLinearGradientPaint);
                    } else if (endX < dotPointX) {
                        RectF rHighlight = new RectF(x, y, endX, y + pitchStickHeight);
                        canvas.drawRoundRect(rHighlight, 8, 8, mHighlightPitchStickLinearGradientPaint);
                    }
                    fineTuneTheHighlightAnimation(endX);
                } else {
                    RectF rNormal = new RectF(x, y, endX, y + pitchStickHeight);
                    canvas.drawRoundRect(rNormal, 8, 8, mPitchStickLinearGradientPaint);
                    if (DEBUG) {
                        mHighlightPitchStickLinearGradientPaint.setTextSize(28);
                        canvas.drawText(tone.word, x, 30, mHighlightPitchStickLinearGradientPaint);
                        canvas.drawText((int) (x) + "", x, 60, mHighlightPitchStickLinearGradientPaint);
                        canvas.drawText((int) (endX) + "", x, 90, mHighlightPitchStickLinearGradientPaint);
                    }
                }
            }
        }
    }
    //</editor-fold>

    private volatile boolean emittingEnabled = false;

    private void fineTuneTheHighlightAnimation(float endX) {
        if (endX > dotPointX) { // Animation Enhancement, If still to far from end, just keep the animation ongoing and update the coordinate
            float value = getYForPitchPivot();
            // It works with an emision range
            int[] location = new int[2];
            this.getLocationInWindow(location);
            if (emittingEnabled && mParticleSystem != null) {
                mParticleSystem.updateEmitPoint((int) (location[0] + dotPointX), location[1] + (int) (value));
            }
        }
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 设置歌词信息
     *
     * @param data 歌词信息对象
     */
    public void setLrcData(@Nullable LrcData data) {
        lrcData = data;
        totalPitch = 0;

        mCurrentTime = 0;
        pitchMax = 0;
        pitchMin = 100;

        currentPitchStartTime = -1;
        currentPitchEndTime = -1;
        currentEntryEndTime = -1;
        everyPitchList.clear();

        cumulatedScore = mInitialScore;
        totalScore = 0;

        mTimestampForFirstNote = -1;
        mInHighlightStatus = false;
        mLocalPitch = 0;
        if (mParticleSystem != null) {
            mParticleSystem.stopEmitting();
        }

        if (lrcData != null && lrcData.entrys != null && !lrcData.entrys.isEmpty()) {
            lrcEndTime = lrcData.entrys.get(lrcData.entrys.size() - 1).getEndTime();
            totalScore = scorePerSentence * lrcData.entrys.size() + mInitialScore;

            for (LrcEntryData entry : lrcData.entrys) {
                for (LrcEntryData.Tone tone : entry.tones) {
                    pitchMin = Math.min(pitchMin, tone.pitch);
                    pitchMax = Math.max(pitchMax, tone.pitch);
                    totalPitch++;
                }
            }

            List<LrcEntryData.Tone> tone = lrcData.entrys.get(0).tones;
            if (tone != null && !tone.isEmpty()) {
                mTimestampForFirstNote = tone.get(0).begin; // find the first note timestamp
                mTimestampForFirstNote = (Math.round((mTimestampForFirstNote / 1000.f)) * 1000); // to make first note indicator animation more smooth
            }
        }

        invalidate();
    }

    private long mTimestampForFirstNote = -1;

    private long mCurrentTime = 0;
    private volatile float mLocalPitch = 0.0F;

    private long mTimestampForLastAnimationDecrease = -1;

    private void setMLocalPitch(float localPitch) {
        this.mLocalPitch = localPitch;
        invalidate();
    }

    /**
     * 根据当前播放时间获取 Pitch，并且更新
     * {@link this#currentPitchStartTime}
     * {@link this#currentPitchEndTime}
     * {@link this#currentEntryEndTime}
     *
     * @return 当前时间歌词的 Pitch
     */
    private float findPitchByTime(long time) {
        if (lrcData == null) return 0;

        float targetPitch = 0;
        int entryCount = lrcData.entrys.size();
        for (int i = 0; i < entryCount; i++) {
            LrcEntryData entry = lrcData.entrys.get(i);
            if (time >= entry.getStartTime() && time <= entry.getEndTime()) { // 索引
                int toneCount = entry.tones.size();
                for (int j = 0; j < toneCount; j++) {
                    LrcEntryData.Tone tone = entry.tones.get(j);
                    if (time >= tone.begin && time <= tone.end) {
                        targetPitch = tone.pitch;
                        currentPitchStartTime = tone.begin;
                        currentPitchEndTime = tone.end;

                        currentEntryEndTime = entry.getEndTime();
                        break;
                    }
                }
                break;
            }
        }
        if (targetPitch == 0) {
            currentPitchStartTime = -1;
            currentPitchEndTime = -1;
            if (time > currentEntryEndTime) {
                currentEntryEndTime = -1;
            }
        } else {
            // 进入此行代码条件 ： 所唱歌词句开始时间 <= 当前时间 >= 所唱歌词句结束时间
            // 强行加上一个　0 分 ，标识此为可打分句
            everyPitchList.put(time, 0d);
        }
        mCurrentPitch = targetPitch;
        return targetPitch;
    }

    /**
     * 更新音调，更新分数，执行圆点动画
     *
     * @param pitch 单位 hz
     */
    public void updateLocalPitch(float pitch) {
        if (lrcData == null) {
            return;
        }
        float currentOriginalPitch = mCurrentPitch;

        if (currentOriginalPitch == 0) {
            return;
        }

        if (pitch < pitchMin || pitch > pitchMax) {
            return;
        }

        double scoreAfterNormalization = calculateScore(mCurrentTime, pitchToTone(pitch), pitchToTone(currentOriginalPitch));

        if (System.currentTimeMillis() - lastCurrentTs > 200) {
            ObjectAnimator.ofFloat(this, "mLocalPitch", this.mLocalPitch, pitch).setDuration(20).start();
            lastCurrentTs = System.currentTimeMillis();

            assureAnimationForPitchPivot(scoreAfterNormalization);
        }
    }

    private long lastCurrentTs = 0;

    private double calculateScore(long currentTime, double singerTone, double desiredTone) {
        double scoreAfterNormalization; // [0, 1]
        double score = 1 - Math.abs(desiredTone - singerTone) / desiredTone;
        // 得分线以下的分数归零
        score = score >= scoreCountLine ? score : 0f;
        scoreAfterNormalization = score;
        // 百分制分数 * 每句固定分数
        score *= scorePerSentence;
        everyPitchList.put(currentTime, score);
        return scoreAfterNormalization;
    }

    private void assureAnimationForPitchPivot(double scoreAfterNormalization) {
        // Animation for particle
        if (scoreAfterNormalization >= 0.9) {
            if (mParticleSystem != null) {
                float value = getYForPitchPivot();
                // It works with an emision range
                int[] location = new int[2];
                this.getLocationInWindow(location);
                if (!emittingEnabled) {
                    emittingEnabled = true;
                    mParticleSystem.emit((int) (location[0] + dotPointX), location[1] + (int) (value), 12);
                } else {
                    mParticleSystem.updateEmitPoint((int) (location[0] + dotPointX), location[1] + (int) (value));
                }
                mParticleSystem.resumeEmitting();
            }
            mInHighlightStatus = true;
        } else {
            if (mParticleSystem != null) {
                mParticleSystem.stopEmitting();
            }
            mInHighlightStatus = false;
        }
    }

    /**
     * 更新当前分数
     *
     * @param time 当前歌曲播放时间 毫秒
     */
    private void updateScore(long time) {
        if (time < mTimestampForFirstNote) { // Not started
            return;
        }

        //  没有开始 || 在空档期
        boolean pushAll = currentEntryEndTime == -1;
        // 当前时间 >= 打分句结束时间
        boolean isThisSentenceOver = time >= currentEntryEndTime;
        // 当前时间 >= 歌词结束时间
        boolean isThisSongOver = time >= lrcEndTime;

        if (pushAll || isThisSentenceOver || isThisSongOver) {
            if (!everyPitchList.isEmpty()) {
                // 计算歌词当前句的分数 = 所有打分/分数个数
                double tempTotalScore = 0;
                int scoreCount = 0;

                Double tempScore;
                // 两种情况 1. 到了空档期 2. 到了下一句
                Iterator<Long> iterator = everyPitchList.keySet().iterator();
                int continuousZeroCount = 0;
                while (iterator.hasNext()) {
                    Long duration = iterator.next();
                    if (pushAll || duration <= currentEntryEndTime) {
                        tempScore = everyPitchList.get(duration);
                        if (tempScore == null || tempScore.floatValue() == 0.f) {
                            continuousZeroCount++;
                            if (continuousZeroCount < 8) {
                                tempScore = null; // Ignore it when not enough continuous zeros
                            } else {
                                continuousZeroCount = 0; // re-count it when reach 8 continuous zeros
                                assureAnimationForPitchPivot(0); // Force stop the animation when reach 8 continuous zeros
                                ObjectAnimator.ofFloat(PitchView.this, "mLocalPitch", PitchView.this.mLocalPitch, PitchView.this.mLocalPitch * 1 / 3, 0.0f).setDuration(200).start(); // Decrease the local pitch pivot
                            }
                        } else {
                            continuousZeroCount = 0;
                        }
                        iterator.remove();
                        everyPitchList.remove(duration);
                        if (tempScore != null) {
                            tempTotalScore += tempScore.floatValue();
                            scoreCount++;
                        }
                    }
                }

                // 获取歌词 pitch 时为了标记可打分句给每句加了 1 个 0 分
                scoreCount = Math.max(1, scoreCount - 1);

                double scoreThisTime = tempTotalScore / scoreCount;
                // 统计到累计分数
                cumulatedScore += scoreThisTime;
                // 回调到上层
                dispatchScore(scoreThisTime);

                if (scoreThisTime == 0 && this.mLocalPitch != 0) {
                    assureAnimationForPitchPivot(0); // Force stop the animation when there is no new score for a long time(a full sentence)
                    ObjectAnimator.ofFloat(PitchView.this, "mLocalPitch", 0.0f).setDuration(10).start(); // Decrease the local pitch pivot
                }
            }
        }
    }

    private boolean mInHighlightStatus;

    /**
     * 根据当前歌曲时间决定是否回调 {@link OnSingScoreListener#onScore(double, double, double)}
     *
     * @param score 本次算法返回的分数
     */
    private void dispatchScore(double score) {
        if (onActionListener != null) {
            onActionListener.onScore(score, cumulatedScore, totalScore);
        }
    }

    /**
     * 更新进度，单位毫秒
     * 根据当前时间，决定是否回调 {@link OnSingScoreListener#onOriginalPitch(float, int)}
     * 与打分逻辑无关
     *
     * @param time 当前播放时间，毫秒
     */
    public void updateTime(long time) {
        if (lrcData == null) {
            return;
        }

        this.mCurrentTime = time;
        if (time < currentPitchStartTime || time > currentPitchEndTime) {
            float currentOriginalPitch = findPitchByTime(time);
            if (currentOriginalPitch > 0) {
                onActionListener.onOriginalPitch(currentOriginalPitch, totalPitch);
            }
        }
        updateScore(time);

        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (onActionListener != null) onActionListener = null;
    }

    public static double pitchToTone(double pitch) {
        double eps = 1e-6;
        return (Math.max(0, Math.log(pitch / 55 + eps) / Math.log(2))) * 12;
    }

    public interface OnSingScoreListener {
        /**
         * 咪咕歌词原始参考 pitch 值回调, 用于开发者自行实现打分逻辑. 歌词每个 tone 回调一次
         *
         * @param pitch      当前 tone 的 pitch 值
         * @param totalCount 整个 xml 的 tone 个数, 用于开发者方便自己在 app 层计算平均分.
         */
        void onOriginalPitch(float pitch, int totalCount);

        /**
         * 歌词组件内置的打分回调, 每句歌词结束的时候提供回调(句指 xml 中的 sentence 节点),
         * 并提供 totalScore 参考值用于按照百分比方式显示分数
         *
         * @param score           这次回调的分数 0-10 之间
         * @param cumulativeScore 累计的分数 初始分累计到当前的分数
         * @param totalScore      总分 初始分(默认值 0 分) + xml 中 sentence 的个数 * 10
         */
        void onScore(double score, double cumulativeScore, double totalScore);
    }
}
