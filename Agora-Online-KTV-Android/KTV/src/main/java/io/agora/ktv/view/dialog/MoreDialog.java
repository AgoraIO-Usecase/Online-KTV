package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.agora.data.model.AgoraMember;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.baselibrary.base.BaseFragment;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.R;
import io.agora.ktv.bean.MusicSettingBean;
import io.agora.ktv.databinding.KtvDialogMoreBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.view.RoomActivity;
import io.agora.ktv.view.fragment.BeautyVoiceFragment;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.RtcEngineEx;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

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
        AgoraMember mMine = RoomManager.Instance(requireContext()).getMine();
        if (mMine == null) {
            ToastUtil.toastShort(requireContext(), "Self AgoraMember is null");
            dismiss();
        }else {
            mBinding.btnCameraDialogMore.setIconResource(mMine.getIsVideoMuted() == 0 ? R.drawable.ic_camera_on : R.drawable.ic_camera_off);

            mBinding.btnVoiceDialogMore.setOnClickListener(this::showVoicePage);
            mBinding.btnEffectDialogMore.setOnClickListener(this::showEffectPage);
            mBinding.btnCameraDialogMore.setOnClickListener(this::toggleCamera);
        }
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

    private void toggleCamera(View v) {
        AgoraMember mMine = RoomManager.Instance(requireContext()).getMine();
        if (mMine == null) {
            return;
        }

        boolean muteThisTime = mMine.getIsVideoMuted() == 0;

        RoomManager.Instance(requireContext())
                .toggleSelfVideo(muteThisTime)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mBinding.btnCameraDialogMore.setEnabled(false);
                    }

                    @Override
                    public void onComplete() {
//                        mMine.setIsVideoMuted(muteThisTime ? 1 : 0);
                        mBinding.btnCameraDialogMore.setEnabled(true);

                        RtcEngineEx engine = RoomManager.Instance(requireContext()).getRtcEngine();
                        engine.enableLocalVideo(!muteThisTime);
//                        engine.muteLocalVideoStream(muteThisTime);
                        mBinding.btnCameraDialogMore.setIconResource(muteThisTime ? R.drawable.ic_camera_off : R.drawable.ic_camera_on);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mBinding.btnCameraDialogMore.setEnabled(true);
                    }
                });
    }
}
