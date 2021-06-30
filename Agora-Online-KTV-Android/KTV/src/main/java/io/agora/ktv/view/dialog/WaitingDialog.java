package io.agora.ktv.view.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogWaitingBinding;

/**
 * Waiting
 *
 * @author chenhengfei@agora.io
 */
public class WaitingDialog extends DataBindBaseDialog<KtvDialogWaitingBinding> {
    private static final String TAG = WaitingDialog.class.getSimpleName();

    private static final String TAG_MSG = "msg";

    private String msg;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mCallback.onTimeout();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.ktv_Dialog_Waiting);
    }

    @Override
    public void iniBundle(@NonNull Bundle bundle) {
        msg = bundle.getString(TAG_MSG);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_dialog_waiting;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {

    }

    @Override
    public void iniData() {
        setCancelable(false);

        mDataBinding.tvMsg.setText(msg);
        mHandler.postDelayed(mRunnable, 60 * 1000L);
    }

    public void show(@NonNull FragmentManager manager, String msg, Callback mCallback) {
        this.mCallback = mCallback;
        Bundle bundle = new Bundle();
        bundle.putString(TAG_MSG, msg);
        setArguments(bundle);
        super.show(manager, TAG);
    }

    private Callback mCallback;

    public interface Callback {
        void onTimeout();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        mHandler.removeCallbacks(mRunnable);
    }
}
