package io.agora.ktv.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.palette.graphics.Palette;

import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvLayoutLrcControlViewBinding;
import io.agora.lrcview.LrcView;
import io.agora.lrcview.PitchView;

/**
 * 歌词控制View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/16
 */
public class LrcControlView extends FrameLayout implements View.OnClickListener {

    protected KtvLayoutLrcControlViewBinding mDataBinding;

    public LrcView getLrcView() {
        return mDataBinding.ilActive.lrcView;
    }

    public PitchView getPitchView() {
        return mDataBinding.ilActive.pitchView;
    }


    public enum Role {
        Singer, Listener
    }

    private Role mRole = Role.Listener;
    private MemberMusicModel mMusic;
    private OnLrcActionListener mOnLrcActionListener;

    public LrcControlView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mDataBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.ktv_layout_lrc_control_view, this, true);

        mDataBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        initListener();
    }

    private void initListener() {
        mDataBinding.ilChorus.btChorus.setOnClickListener(this);
        mDataBinding.ilActive.switchOriginal.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicMenu.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicStart.setOnClickListener(this);
        mDataBinding.ilActive.ivChangeSong.setOnClickListener(this);
    }

    public void setOnLrcClickListener(OnLrcActionListener mOnLrcActionListener) {
        this.mOnLrcActionListener = mOnLrcActionListener;
        mDataBinding.ilActive.lrcView.setActionListener(this.mOnLrcActionListener);
    }

    private CountDownTimer mCountDownLatch;

    private void startTimer() {
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
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        if (mRole == Role.Singer) {
            mDataBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_wait_join_chorus, 0, 20));
            mDataBinding.ilChorus.btChorus.setText(R.string.ktv_music_chorus_start_now);
            mDataBinding.ilChorus.btChorus.setBackgroundResource(R.drawable.ktv_shape_wait_chorus_button);
        } else if (mRole == Role.Listener) {
            mDataBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_join_chorus_, 0, 20));
            mDataBinding.ilChorus.btChorus.setText(R.string.ktv_music_join_chorus);
            mDataBinding.ilChorus.btChorus.setBackgroundResource(R.drawable.ktv_shape_start_chorus_button);
        }

        if (mRole == Role.Singer) {
            startTimer();
        }
    }

    public void onMemberJoinedChorus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        stopTimer();
    }

    public void onPrepareStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        if (this.mRole == Role.Singer) {
            mDataBinding.ilActive.lrcView.setEnableDrag(true);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mDataBinding.ilActive.lrcView.setEnableDrag(false);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }

        stopTimer();
    }

    public void onPlayStatus() {
        stopTimer();

        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_pause);
    }

    public void onPauseStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mDataBinding.clActive.setVisibility(View.VISIBLE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_play);
    }

    public void onIdleStatus() {
        mDataBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mDataBinding.clActive.setVisibility(View.GONE);
        mDataBinding.ilChorus.getRoot().setVisibility(View.GONE);
        mDataBinding.ilPrepare.getRoot().setVisibility(View.GONE);
        mDataBinding.ilActive.getRoot().setVisibility(View.GONE);

        stopTimer();
    }

    public void setRole(@NonNull Role mRole) {
        this.mRole = mRole;

        if (this.mRole == Role.Singer) {
            mDataBinding.ilActive.lrcView.setEnableDrag(true);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.switchOriginal.setChecked(true);
        } else if (this.mRole == Role.Listener) {
            mDataBinding.ilActive.lrcView.setEnableDrag(false);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
        }
    }

    public void setMusic(@NonNull MemberMusicModel mMusic) {
        mDataBinding.ilActive.lrcView.reset();
        mDataBinding.ilActive.pitchView.reset();

        this.mMusic = mMusic;
        mDataBinding.tvMusicName.setText(this.mMusic.getName());
        mDataBinding.ilChorus.tvMusicName2.setText(this.mMusic.getName());
    }

    public void setCountDown(int time) {
        if (mRole == Role.Singer) {
            mDataBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_wait_join_chorus, 0, time));
        } else if (mRole == Role.Listener) {
            mDataBinding.ilChorus.tvWaitingTime.setText(
                    getContext().getString(R.string.ktv_room_time_join_chorus_, 0, time));
        }
    }

    public void setLrcViewBackground(@DrawableRes int resId) {
//        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), resId);
//        Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
//            @Override
//            public void onGenerated(@Nullable Palette palette) {
//                if (palette == null) {
//                    return;
//                }
//
//                int defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_highligh);
//                mDataBinding.ilActive.lrcView.setCurrentColor(palette.getLightVibrantColor(defaultColor));
//
//                defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_nomal);
//                mDataBinding.ilActive.lrcView.setNormalColor(palette.getLightMutedColor(defaultColor));
//            }
//        });
        mDataBinding.clActive.setBackgroundResource(resId);
    }

    @Override
    public void onClick(View v) {
        if (v == mDataBinding.ilActive.switchOriginal) {
            mOnLrcActionListener.onSwitchOriginalClick();
        } else if (v == mDataBinding.ilActive.ivMusicMenu) {
            mOnLrcActionListener.onMenuClick();
        } else if (v == mDataBinding.ilActive.ivMusicStart) {
            mOnLrcActionListener.onPlayClick();
        } else if (v == mDataBinding.ilActive.ivChangeSong) {
            mOnLrcActionListener.onChangeMusicClick();
        } else if (v == mDataBinding.ilChorus.btChorus) {
            if (mRole == Role.Singer) {
                mOnLrcActionListener.onStartSing();
            } else if (mRole == Role.Listener) {
                mOnLrcActionListener.onJoinChorus();
            }
        }
    }

    public void setSwitchOriginalChecked(boolean checked) {
        mDataBinding.ilActive.switchOriginal.setChecked(checked);
    }

    public interface OnLrcActionListener extends LrcView.OnActionListener {
        void onSwitchOriginalClick();

        void onMenuClick();

        void onPlayClick();

        void onChangeMusicClick();

        void onStartSing();

        void onJoinChorus();

        void onWaitTimeOut();

        void onCountTime(int time);
    }
}
