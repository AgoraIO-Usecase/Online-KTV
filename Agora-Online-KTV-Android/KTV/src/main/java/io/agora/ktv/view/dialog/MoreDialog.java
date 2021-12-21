package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

        // Edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.getRoot().setPadding(inset.left, 0, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        initView();
    }

    private void initView() {
        mBinding.btnVoiceDialogMore.setOnClickListener(this::showVoicePage);
        mBinding.btnEffectDialogMore.setOnClickListener(this::showEffectPage);
//        mBinding.btnCameraDialogMore.setOnClickListener(this::toggleCamera);
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
