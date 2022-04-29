package io.agora.ktv.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import java.text.DecimalFormat;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.ktv.databinding.KtvLayoutLrcPrepareBinding;
import io.agora.ktv.databinding.KtvLayoutLrcControlViewBinding;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.lrcview.LrcView;
import io.agora.lrcview.PitchView;

/**
 * 歌词控制View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/16
 */
public class LrcControlView extends FrameLayout implements View.OnClickListener {

    protected KtvLayoutLrcControlViewBinding mBinding;
    protected KtvLayoutLrcPrepareBinding mPrepareBinding;

    public LrcView getLrcView() {
        return mBinding.ilActive.lrcView;
    }

    public PitchView getPitchView() {
        return mBinding.ilActive.pitchView;
    }


    public enum Role {
        Singer, Listener
    }

    private Role mRole = Role.Listener;
    private MemberMusicModel mMusic;
    private OnLrcActionListener mOnLrcActionListener;
    private Handler mHandler = new Handler(Looper.myLooper());

    private double localPitch = 0;
    private double musicPitch = 0;

    public LrcControlView(@NonNull Context context) {
        this(context, null);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mBinding = KtvLayoutLrcControlViewBinding.inflate(LayoutInflater.from(context),this, true);
//        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.ktv_layout_lrc_control_view, this, true);

        mPrepareBinding = KtvLayoutLrcPrepareBinding.bind(mBinding.getRoot());

        mBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        initListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBinding = null;
        mPrepareBinding = null;
    }

    private void initListener() {
        mBinding.ilChorus.btChorus.setOnClickListener(this);
        mBinding.ilActive.switchOriginal.setOnClickListener(this);
        mBinding.ilActive.ivMusicMenu.setOnClickListener(this);
        mBinding.ilActive.ivMusicStart.setOnClickListener(this);
        mBinding.ilActive.ivChangeSong.setOnClickListener(this);

    }

    public void setOnLrcClickListener(OnLrcActionListener mOnLrcActionListener) {
        this.mOnLrcActionListener = mOnLrcActionListener;
        mBinding.ilActive.lrcView.setActionListener(this.mOnLrcActionListener);
    }

    public void setPitchViewOnActionListener(PitchView.OnActionListener onActionListener){
        mBinding.ilActive.pitchView.onActionListener = onActionListener;
    }

    private CountDownTimer mCountDownLatch;

    private void startTimer() {
        if(mCountDownLatch != null) mCountDownLatch.cancel();

        mCountDownLatch = new CountDownTimer(20 * 1000, 999) {
            @Override
            public void onTick(long millisUntilFinished) {
                int second = (int) (millisUntilFinished / 1000);

                if (mOnLrcActionListener != null) {
                    mOnLrcActionListener.onCountTime(second);
                }

                setCountDown(second);
            }

            @Override
            public void onFinish() {
                mOnLrcActionListener.onWaitTimeOut();
            }
        }.start();
    }

    private void stopTimer() {
        if (mCountDownLatch != null) {
            mCountDownLatch.cancel();
            mCountDownLatch = null;
        }
    }

    public void onWaitChorusStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mBinding.ilChorus.getRoot().setVisibility(View.VISIBLE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        if (mRole == Role.Singer) {
            mBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_wait_join_chorus, 0, 20));
            mBinding.ilChorus.btChorus.setText(R.string.ktv_music_chorus_start_now);
            mBinding.ilChorus.btChorus.setStrokeWidth((int) KTVUtil.dp2px(1));
            mBinding.ilChorus.btChorus.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));

        } else if (mRole == Role.Listener) {
            mBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_join_chorus_, 0, 20));
            mBinding.ilChorus.btChorus.setText(R.string.ktv_music_join_chorus);
            mBinding.ilChorus.btChorus.setStrokeWidth(0);
            mBinding.ilChorus.btChorus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.ktv_colorAccent)));
        }

        if (mRole == Role.Singer) {
            startTimer();
        }
    }

    public void onMemberJoinedChorus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.VISIBLE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        stopTimer();
    }

    public void onPrepareStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.VISIBLE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        if (this.mRole == Role.Singer) {
            mBinding.ilActive.lrcView.setEnableDrag(true);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mBinding.ilActive.lrcView.setEnableDrag(false);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }

        stopTimer();
    }

    public void onPlayStatus() {
        stopTimer();

        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mBinding.ilActive.ivMusicStart.setIconResource(R.drawable.ktv_ic_pause);
//        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_pause);
    }

    public void onPauseStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mBinding.ilActive.ivMusicStart.setIconResource(R.drawable.ktv_ic_play);
//        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_play);
    }

    public void onIdleStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mBinding.clActive.setVisibility(View.GONE);
        mBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        stopTimer();
    }

    public void setRole(@NonNull Role mRole) {
        this.mRole = mRole;

        if (this.mRole == Role.Singer) {
            mBinding.ilActive.lrcView.setEnableDrag(true);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mBinding.ilActive.lrcView.setEnableDrag(false);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }
    }

    public void setMusic(@NonNull MemberMusicModel mMusic) {
        mBinding.ilActive.lrcView.reset();
        mBinding.ilActive.pitchView.setLrcData(null);

        mBinding.tvMusicName.setText(mMusic.getName());
        mBinding.ilChorus.tvMusicName2.setText(mMusic.getName());
    }

    public void setCountDown(int time) {
        if (mRole == Role.Singer) {
            mBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_wait_join_chorus, 0, time));
        } else if (mRole == Role.Listener) {
            mBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_join_chorus_, 0, time));
        }
    }

    public void setLocalPitch(double pitch) {
        localPitch = pitch;
        updateName();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(localPitch == pitch){
                    localPitch = 0;
                }
            }
        }, 1000l);
    }

    public void setMusicPitch(double pitch) {
        musicPitch = pitch;
        updateName();
    }

    private void updateName() {
        if(mMusic == null)
            return;
        StringBuffer buf = new StringBuffer();
        buf.append(mMusic.getName());
        if (localPitch > 0) buf.append(" local: " + new DecimalFormat("#.0").format(localPitch));
        if (musicPitch > 0) buf.append(" music: " + musicPitch);
        mBinding.tvMusicName.setText(buf.toString());
        if(Math.abs(localPitch - musicPitch) < 20){
            mBinding.ivGood.setVisibility(VISIBLE);
        }
        else {
            mBinding.ivGood.setVisibility(INVISIBLE);
        }
    }

    public void setLrcViewBackground(@DrawableRes int resId) {
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), resId);
        Palette.from(mBitmap).generate(palette -> {
            if (palette == null) {
                return;
            }

            int defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_highligh);
            mBinding.ilActive.lrcView.setCurrentColor(palette.getLightVibrantColor(defaultColor));

            defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_nomal);
            mBinding.ilActive.lrcView.setNormalColor(palette.getLightMutedColor(defaultColor));
        });
        mBinding.clActive.setBackgroundResource(resId);
    }

    @Override
    public void onClick(View v) {
        if (v == mBinding.ilActive.switchOriginal) {
            mOnLrcActionListener.onSwitchOriginalClick();
        } else if (v == mBinding.ilActive.ivMusicMenu) {
            mOnLrcActionListener.onMenuClick();
        } else if (v == mBinding.ilActive.ivMusicStart) {
            mOnLrcActionListener.onPlayClick();
        } else if (v == mBinding.ilActive.ivChangeSong) {
            mOnLrcActionListener.onChangeMusicClick();
        } else if (v == mBinding.ilChorus.btChorus) {
            if (mRole == Role.Singer) {
                mOnLrcActionListener.onStartSing();
            } else if (mRole == Role.Listener) {
                mOnLrcActionListener.onJoinChorus();
            }
        }
    }

    public void setSwitchOriginalChecked(boolean checked) {
        mBinding.ilActive.switchOriginal.setChecked(checked);
    }

    public interface OnLrcActionListener extends LrcView.OnActionListener {
        default void onSwitchOriginalClick(){ }

        default void onMenuClick() { }

        default void onPlayClick() { }

        default void onChangeMusicClick() { }

        default void onStartSing() { }

        default void onJoinChorus() { }

        default void onWaitTimeOut() { }

        default void onCountTime(int time) { }
    }
}
