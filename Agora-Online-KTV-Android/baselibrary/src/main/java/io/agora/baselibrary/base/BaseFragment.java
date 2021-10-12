package io.agora.baselibrary.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import io.agora.baselibrary.util.KTVUtil;

/**
 * On Jetpack navigation
 * Fragments enter/exit represent onCreateView/onDestroyView
 * Thus we should detach all reference to the VIEW on onDestroyView
 */
public abstract class BaseFragment<B extends ViewBinding> extends Fragment {
    public B mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = getViewBindingByReflect(inflater, container);
        if (mBinding == null)
            return null;
        return mBinding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    public void showLoading(boolean cancelable) {
        getParentActivity().showLoadingDialog(cancelable);
    }

    public void dismissLoading() {
        getParentActivity().dismissLoading();
    }

    public BaseActivity<?> getParentActivity() {
        return (BaseActivity<?>) requireActivity();
    }


    public B getViewBindingByReflect(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        try {
            Class<B> c = KTVUtil.getGenericClass(getClass(), 0);
            return (B) KTVUtil.getViewBinding(c, inflater, container);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}