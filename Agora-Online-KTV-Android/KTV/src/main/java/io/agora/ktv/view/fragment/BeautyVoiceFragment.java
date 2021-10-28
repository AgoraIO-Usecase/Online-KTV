package io.agora.ktv.view.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.chip.Chip;

import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.util.KTVUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MusicSettingBean;
import io.agora.ktv.databinding.KtvFragmentBeautyVoiceBinding;

public class BeautyVoiceFragment extends BaseFragment<KtvFragmentBeautyVoiceBinding> {
    public static final String TAG = "BeautyVoiceFragment";
    private final MusicSettingBean mSetting;

    public BeautyVoiceFragment(MusicSettingBean mSetting) {
        this.mSetting = mSetting;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        String[] stringArray = getResources().getStringArray(R.array.ktv_audioPreset);
        for (String s : stringArray) {
            Chip chip = new Chip(requireContext());
            chip.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            chip.setChipBackgroundColor(AppCompatResources.getColorStateList(requireContext(), R.color.selector_chip_color));
            chip.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.selector_chip_text_color));
            chip.setText(s);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipStartPadding(KTVUtil.dp2px(12));
            chip.setChipEndPadding(KTVUtil.dp2px(12));
            mBinding.getRoot().addView(chip);
        }

        try {
            ((Chip) mBinding.getRoot().getChildAt(mSetting.getEffect())).setChecked(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mBinding.getRoot().setOnCheckedChangeListener((group, checkedId) -> {
            int childCount = group.getChildCount();
            int checkedIndex = -1;
            for (int i = 0; i < childCount; i++) {
                if (((Chip) group.getChildAt(i)).isSelected()) {
                    checkedIndex = i;
                    break;
                }
            }
            if (checkedIndex > -1)
                mSetting.setEffect(checkedIndex);
        });
    }
}
