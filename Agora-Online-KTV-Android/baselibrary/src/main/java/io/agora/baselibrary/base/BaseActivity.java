package io.agora.baselibrary.base;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.Type;

import io.agora.baselibrary.util.KTVUtil;
import io.agora.baselibrary.util.ToastUtil;


/**
 * 基础
 *
 * @author chenhengfei@agora.io
 */

public abstract class BaseActivity<B extends ViewBinding> extends AppCompatActivity {
    public B mBinding;
    private AlertDialog mLoadingDialog = null;
    private long time;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        mBinding = getViewBindingByReflect(getLayoutInflater());
        if(mBinding == null) {
            ToastUtil.toastShort(this, "Inflate Error");
            finish();
        }else
            setContentView(mBinding.getRoot());

//        WindowCompat.setDecorFitsSystemWindows(getWindow(), true)
    }

    @Override
    protected void onResume() {
        super.onResume();
        KTVUtil.logD("launch this activity need "+(System.currentTimeMillis() - time)+"ms" );
    }

    public void showLoadingDialog() {
        showLoadingDialog(true);
    }

    public void showLoadingDialog(Boolean cancelable) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new AlertDialog.Builder(this).create();
            mLoadingDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true);
            progressBar.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            mLoadingDialog.setView(progressBar);
        }
        mLoadingDialog.setCancelable(cancelable);
        mLoadingDialog.show();
    }

    public void dismissLoading() {
        if(mLoadingDialog != null)
            mLoadingDialog.dismiss();
    }

    @SuppressWarnings("unchecked")
    private B getViewBindingByReflect(@NonNull LayoutInflater inflater) {
        try {
            Type type = getClass().getGenericSuperclass();
            if(type == null) return null;
            Class<B> c = KTVUtil.getGenericClass(getClass(),0);
            return (B) KTVUtil.getViewBinding(c, inflater);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}