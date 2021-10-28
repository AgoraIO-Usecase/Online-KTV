package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.baselibrary.base.BaseFragment;
import io.agora.ktv.bean.MusicSettingBean;
import io.agora.ktv.databinding.KtvDialogMoreBinding;
import io.agora.ktv.view.fragment.BeautyVoiceFragment;

public class MoreDialog extends BaseBottomSheetDialogFragment<KtvDialogMoreBinding> {
    public static final String TAG = "MoreDialog";

    private final MusicSettingBean mSetting;

    public MoreDialog(MusicSettingBean mSetting) {
        this.mSetting = mSetting;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mBinding.chipVoiceDialogMore.setOnClickListener(this::showVoicePage);
        mBinding.chipEffectDialogMore.setOnClickListener(this::showEffectPage);
    }

    private void showVoicePage(View v) {
        mBinding.getRoot().removeAllViews();
        BaseFragment<?> voiceFragment = new BeautyVoiceFragment(mSetting);
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(mBinding.getRoot().getId(), voiceFragment, BeautyVoiceFragment.TAG);
        ft.commit();
    }

    private void showEffectPage(View v) {
    }
}
