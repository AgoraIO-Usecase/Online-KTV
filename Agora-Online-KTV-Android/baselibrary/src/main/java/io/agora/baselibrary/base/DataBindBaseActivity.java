package io.agora.baselibrary.base;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

/**
 * 基础
 *
 * @author chenhengfei@agora.io
 */
public abstract class DataBindBaseActivity<V extends ViewDataBinding> extends BaseActivity {

    protected V mDataBinding;

    @Override
    protected void setCusContentView() {
        mDataBinding = DataBindingUtil.setContentView(this, getLayoutId());
    }
}