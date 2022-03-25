package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.ktv.bean.MusicSettingBean;
import io.agora.ktv.databinding.KtvDialogMusicSettingBinding;

/**
 * 歌曲菜单
 *
 * @author chenhengfei@agora.io
 */
public class MusicSettingDialog extends BaseBottomSheetDialogFragment<KtvDialogMusicSettingBinding> {
    public static String TAG = "MusicSettingDialog";
    private final MusicSettingBean mSetting;

    /**
     * FIXME Not recommend
     */
    public MusicSettingDialog(MusicSettingBean mSetting) {
        this.mSetting = mSetting;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(mBinding.getRoot().getPaddingLeft(), mBinding.getRoot().getPaddingTop(), mBinding.getRoot().getPaddingBottom(), inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mBinding.switchEar.setChecked(this.mSetting.isEar());
        mBinding.sbVol1.setValue(this.mSetting.getVolMic());
        mBinding.sbVol2.setValue(this.mSetting.getVolMusic());

        // 升降调
        tuningTone(null);
        mBinding.btnToneDownDialogSetting.setOnClickListener(v -> tuningTone(false));
        mBinding.btnToneUpDialogSetting.setOnClickListener(v -> tuningTone(true));

        mBinding.switchEar.setOnCheckedChangeListener((buttonView, isChecked) -> this.mSetting.setEar(isChecked));
        mBinding.sbVol1.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMic((int) value));
        mBinding.sbVol1.setLabelFormatter(value -> String.valueOf((int) value));
        mBinding.sbVol2.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMusic((int) value));
        mBinding.sbVol2.setLabelFormatter(value -> String.valueOf((int) value));
    }

    /**
     * IMediaPlayer.java
     *  /**
     * Sets the pitch of the current media file.
     * pitch Sets the pitch of the local music file by chromatic scale. The default value is 0,
     * which means keeping the original pitch. The value ranges from -12 to 12, and the pitch value
     * between consecutive values is a chromatic value. The greater the absolute value of this
     * parameter, the higher or lower the pitch of the local music file.
     * *
     * - 0: Success.
     * - < 0: Failure.
     * int setAudioMixingPitch(int pitch);
     *
     *
     *
     * @param toneUp true -> +1 | false -> -1 | null -> update value
     */
    private void tuningTone(Boolean toneUp){
        int newToneValue = this.mSetting.getToneValue();
        if (toneUp != null) {
            if (toneUp) {
                newToneValue++;
            } else {
                newToneValue--;
            }

            if (newToneValue > 12)
                newToneValue = 12;

            if (newToneValue < -12)
                newToneValue = -12;

            if (newToneValue != this.mSetting.getToneValue())
                this.mSetting.setToneValue(newToneValue);
        }
        mBinding.textToneDialogSetting.setText(String.valueOf(newToneValue));
    }

    public interface Callback {
        void onEarChanged(boolean isEar);

        void onMicVolChanged(int vol);

        void onMusicVolChanged(int vol);

        void onEffectChanged(int effect);

        void onToneChanged(int newToneValue);
    }
}
