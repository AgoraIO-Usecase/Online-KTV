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

    public enum Status {
        IDLE, WaitChorus, Prepare, Play, Pause
    }

    public enum Role {
        Singer, Listener
    }

    private Status mStatus = Status.IDLE;
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
        mDataBinding.ilActive.switchOriginal.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicMenu.setOnClickListener(this);
        mDataBinding.ilActive.ivMusicStart.setOnClickListener(this);
        mDataBinding.ilActive.ivChangeSong.setOnClickListener(this);
    }

    public void setOnLrcClickListener(OnLrcActionListener mOnLrcActionListener) {
        this.mOnLrcActionListener = mOnLrcActionListener;
        mDataBinding.ilActive.lrcView.setOnSeekBarChangeListener(this.mOnLrcActionListener);
    }

    private CountDownTimer mCountDownLatch;

    private void startTimer() {
        mCountDownLatch = new CountDownTimer(20 * 1000, 999) {
            @Override
            public void onTick(long millisUntilFinished) {
                int second = (int) (millisUntilFinished / 1000);

                if (mRole == Role.Singer) {
                    mDataBinding.ilActive.tvWaitingTime.setText(
                            getContext().getString(R.string.ktv_room_time_wait_join_chorus, "00:" + second));
                } else if (mRole == Role.Listener) {
                    mDataBinding.ilActive.tvWaitingTime.setText(
                            getContext().getString(R.string.ktv_room_time_join_chorus_, "00:" + second));
                }
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

    public void setStatus(@NonNull Status mStatus) {
        this.mStatus = mStatus;
        if (this.mStatus == Status.IDLE) {
            mDataBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
            mDataBinding.ilActive.getRoot().setVisibility(View.GONE);
        } else if (this.mStatus == Status.WaitChorus) {
            mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
            mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

            mDataBinding.ilActive.lrcView.setVisibility(View.GONE);
            mDataBinding.ilActive.llJoinChorus.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);

            if (mRole == Role.Singer) {
                mDataBinding.ilActive.tvWaitingTime.setText(
                        getContext().getString(R.string.ktv_room_time_wait_join_chorus, "00:20"));
                mDataBinding.ilActive.btStart.setBackgroundResource(R.drawable.ktv_shape_wait_chorus_button);
            } else if (mRole == Role.Listener) {
                mDataBinding.ilActive.tvWaitingTime.setText(
                        getContext().getString(R.string.ktv_room_time_join_chorus_, "00:20"));
                mDataBinding.ilActive.btStart.setBackgroundResource(R.drawable.ktv_shape_start_chorus_button);
            }

            startTimer();
        } else if (this.mStatus == Status.Prepare) {
            mDataBinding.ilIDLE.getRoot().setVisibility(View.GONE);
            mDataBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

            mDataBinding.ilActive.lrcView.setVisibility(View.VISIBLE);
            mDataBinding.ilActive.llJoinChorus.setVisibility(View.GONE);
            mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);

            if (this.mRole == Role.Singer) {
                mDataBinding.ilActive.lrcView.setEnableDrag(true);
                mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
                mDataBinding.ilActive.switchOriginal.setChecked(true);
            } else if (this.mRole == Role.Listener) {
                mDataBinding.ilActive.lrcView.setEnableDrag(false);
                mDataBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
            }

            stopTimer();
        } else if (this.mStatus == Status.Play) {
            mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_pause);
        } else if (this.mStatus == Status.Pause) {
            mDataBinding.ilActive.ivMusicStart.setImageResource(R.mipmap.ktv_room_music_play);
        }
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
        this.mMusic = mMusic;
        mDataBinding.ilActive.tvMusicName.setText(this.mMusic.getName());
    }

    public void setLrcViewBackground(@DrawableRes int resId) {
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), resId);
        Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if (palette == null) {
                    return;
                }

                int defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_highligh);
                mDataBinding.ilActive.lrcView.setCurrentColor(palette.getLightVibrantColor(defaultColor));

                defaultColor = ContextCompat.getColor(getContext(), R.color.ktv_lrc_nomal);
                mDataBinding.ilActive.lrcView.setNormalColor(palette.getLightMutedColor(defaultColor));
            }
        });
        mDataBinding.ilActive.rlSing.setBackgroundResource(resId);
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
        }
    }

    public void setSwitchOriginalChecked(boolean checked) {
        mDataBinding.ilActive.switchOriginal.setChecked(checked);
    }

    public interface OnLrcActionListener extends LrcView.OnSeekBarChangeListener {
        void onSwitchOriginalClick();

        void onMenuClick();

        void onPlayClick();

        void onChangeMusicClick();

        void onWaitTimeOut();
    }
}
