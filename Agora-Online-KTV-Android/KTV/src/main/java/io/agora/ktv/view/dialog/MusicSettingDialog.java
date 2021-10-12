package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public MusicSettingDialog(MusicSettingBean mSetting) {
        this.mSetting = mSetting;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.switchEar.setChecked(this.mSetting.isEar());
        mBinding.sbVol1.setValue(this.mSetting.getVolMic());
        mBinding.sbVol2.setValue(this.mSetting.getVolMusic());

        mBinding.switchEar.setOnCheckedChangeListener((buttonView, isChecked) -> this.mSetting.setEar(isChecked));
        mBinding.sbVol1.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMic((int) value));
        mBinding.sbVol2.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMusic((int) value));
    }

    public interface Callback {
        void onEarChanged(boolean isEar);

        void onMicVolChanged(int vol);

        void onMusicVolChanged(int vol);

        void onEffectChanged(int effect);
    }
}
