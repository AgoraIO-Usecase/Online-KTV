package io.agora.ktv.view.dialog;

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

import com.agora.data.model.AgoraMember;
import com.agora.data.sync.RoomManager;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogUserSeatMenuBinding;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 房间用户菜单
 *
 * @author chenhengfei@agora.io
 */
public class UserSeatMenuDialog extends DataBindBaseDialog<KtvDialogUserSeatMenuBinding> implements View.OnClickListener {
    private static final String TAG = UserSeatMenuDialog.class.getSimpleName();

    private static final String TAG_USER = "user";

    private AgoraMember mMember;

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
        mMember = bundle.getParcelable(TAG_USER);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_dialog_user_seat_menu;
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
//        User mUser = mMember.getUserId();
//        mDataBinding.tvName.setText(mUser.getName());
//        Glide.with(this)
//                .load(mUser.getAvatarRes())
//                .placeholder(R.mipmap.default_head)
//                .error(R.mipmap.default_head)
//                .circleCrop()
//                .into(mDataBinding.ivUser);
    }

    public void show(@NonNull FragmentManager manager, AgoraMember data) {
        Bundle intent = new Bundle();
        intent.putParcelable(TAG_USER, data);
        setArguments(intent);
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
                .changeRole(mMember, AgoraMember.Role.Listener.getValue())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        mDataBinding.btSeatoff.setEnabled(true);
                        dismiss();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }
}
