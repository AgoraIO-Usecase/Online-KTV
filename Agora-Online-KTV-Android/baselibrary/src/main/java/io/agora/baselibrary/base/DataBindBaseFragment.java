package io.agora.baselibrary.base;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

/**
 * 基础
 *
 * @author chenhengfei@agora.io
 */
public abstract class DataBindBaseFragment<V extends ViewDataBinding> extends BaseFragment {

    protected V mDataBinding;


    @Override
    public View setCusContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        mDataBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, true);
        return mDataBinding.getRoot();
    }
}