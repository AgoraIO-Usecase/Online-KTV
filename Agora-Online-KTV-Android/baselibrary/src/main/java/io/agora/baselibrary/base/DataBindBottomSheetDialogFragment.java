package io.agora.baselibrary.base;

import android.os.Bundle;
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
public abstract class DataBindBottomSheetDialogFragment<V extends ViewDataBinding> extends BaseBottomSheetDialogFragment {

    protected V mDataBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mDataBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, true);
        return mDataBinding.getRoot();
    }
}