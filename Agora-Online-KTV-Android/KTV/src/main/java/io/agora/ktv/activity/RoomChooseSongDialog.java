package io.agora.ktv.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.agora.data.BaseError;
import com.agora.data.manager.RoomManager;
import com.agora.data.model.Member;
import com.agora.data.model.User;
import com.agora.data.observer.DataCompletableObserver;
import com.bumptech.glide.Glide;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.baselibrary.util.ToastUtile;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogChooseSongBinding;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 点歌菜单
 *
 * @author chenhengfei@agora.io
 */
public class RoomChooseSongDialog extends DataBindBaseDialog<KtvDialogChooseSongBinding> implements View.OnClickListener {
    private static final String TAG = RoomChooseSongDialog.class.getSimpleName();

    private static final String TAG_USER = "user";

    private Member mMember;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Window win = getDialog().getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        win.setAttributes(params);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Dialog_Bottom);
    }

    @Override
    public void iniBundle(@NonNull Bundle bundle) {
        mMember = (Member) bundle.getSerializable(TAG_USER);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_dialog_choose_song;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {
        mDataBinding.btSeatoff.setOnClickListener(this);
    }

    @Override
    public void iniData() {
        User mUser = mMember.getUserId();
        mDataBinding.tvName.setText(mUser.getName());
        Glide.with(this)
                .load(mUser.getAvatarRes())
                .placeholder(R.mipmap.default_head)
                .error(R.mipmap.default_head)
                .circleCrop()
                .into(mDataBinding.ivUser);
    }

    public void show(@NonNull FragmentManager manager) {
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btSeatoff) {
            seatOff();
        }
    }

    private void seatOff() {
        mDataBinding.btSeatoff.setEnabled(false);
        RoomManager.Instance(requireContext())
                .seatOff(mMember, Member.Role.Listener)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mLifecycleProvider.bindToLifecycle())
                .subscribe(new DataCompletableObserver(requireContext()) {
                    @Override
                    public void handleError(@NonNull BaseError e) {
                        mDataBinding.btSeatoff.setEnabled(true);
                        ToastUtile.toastShort(requireContext(), e.getMessage());
                    }

                    @Override
                    public void handleSuccess() {
                        mDataBinding.btSeatoff.setEnabled(true);
                        dismiss();
                    }
                });
    }
}
