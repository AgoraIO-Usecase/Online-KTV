package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.LabelFormatter;

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
        mBinding.ctrlEffect.setSelection(mSetting.getEffect());

        mBinding.switchEar.setOnCheckedChangeListener((buttonView, isChecked) -> this.mSetting.setEar(isChecked));
        mBinding.sbVol1.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMic((int) value));
        mBinding.sbVol1.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        mBinding.sbVol2.addOnChangeListener((slider, value, fromUser) -> this.mSetting.setVolMusic((int) value));
        mBinding.sbVol2.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        mBinding.ctrlEffect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSetting.setEffect(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public interface Callback {
        void onEarChanged(boolean isEar);

        void onMicVolChanged(int vol);

        void onMusicVolChanged(int vol);

        void onEffectChanged(int effect);
    }
}
