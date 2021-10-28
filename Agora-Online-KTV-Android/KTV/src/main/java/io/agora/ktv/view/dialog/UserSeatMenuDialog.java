package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.data.model.AgoraMember;
import com.agora.data.model.User;
import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogUserSeatMenuBinding;
import io.agora.ktv.manager.RoomManager;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 房间用户菜单
 *
 * @author chenhengfei@agora.io
 */
public class UserSeatMenuDialog extends BaseBottomSheetDialogFragment<KtvDialogUserSeatMenuBinding> {
    public static final String TAG = UserSeatMenuDialog.class.getSimpleName();
    private final AgoraMember mMember;

    public UserSeatMenuDialog(AgoraMember mMember) {
        this.mMember = mMember;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mBinding.btSeatoff.setOnClickListener(this::seatOff);
        User mUser = mMember.getUser();
        mBinding.tvName.setText(mUser.getName());
        Glide.with(this)
                .load(mUser.getAvatarRes())
                .into(mBinding.ivUser);
    }

    private void seatOff(View v) {
        v.setEnabled(false);
        RoomManager.Instance(requireContext())
                .changeRole(mMember, AgoraMember.Role.Listener.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        v.setEnabled(true);
                        dismiss();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }
}
