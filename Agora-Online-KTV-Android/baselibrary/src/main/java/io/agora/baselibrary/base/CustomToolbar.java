package io.agora.baselibrary.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import io.agora.baselibrary.R;

/**
 * 自定义toolbar
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/6/8
 */
public class CustomToolbar extends Toolbar {
    private TextView tvTitle;
    private CharSequence mTitleText;

    private Drawable iconBack;

    @ColorInt
    private int mTitleTextColor = Color.BLACK;

    public CustomToolbar(Context context) {
        this(context, null);
    }

    public CustomToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomToolbar);
        CharSequence title = typedArray.getText(R.styleable.CustomToolbar_ct_title);
        setTitle(title);

        iconBack = typedArray.getDrawable(R.styleable.CustomToolbar_ct_icon_back);
        boolean showBack = typedArray.getBoolean(R.styleable.CustomToolbar_ct_showback, true);
        setShowBack(showBack);

        mTitleTextColor = typedArray.getColor(R.styleable.CustomToolbar_ct_color_title, 0xffffffff);
        setTitleTextColor(mTitleTextColor);

        typedArray.recycle();
    }

    @Override
    public void setTitle(@StringRes int resId) {
        setTitle(getResources().getString(resId));
    }

    @Override
    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            if (tvTitle == null) {
                tvTitle = new AppCompatTextView(getContext());
                tvTitle.setSingleLine();
                tvTitle.setEllipsize(TextUtils.TruncateAt.END);
                tvTitle.setGravity(Gravity.CENTER);
                tvTitle.setTextColor(mTitleTextColor);
                tvTitle.setTextSize(20);
            }

            if (indexOfChild(tvTitle) < 0) {
                addSystemView(tvTitle);
            }
        } else {
            if (tvTitle != null) {
                removeView(tvTitle);
            }
        }

        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        mTitleText = title;
    }

    private void addSystemView(View v) {
        final ViewGroup.LayoutParams vlp = v.getLayoutParams();
        final LayoutParams lp;
        if (vlp == null) {
            lp = generateDefaultLayoutParams();
        } else if (!checkLayoutParams(vlp)) {
            lp = generateLayoutParams(vlp);
        } else {
            lp = (LayoutParams) vlp;
        }

        lp.gravity = Gravity.CENTER;
        addView(v, lp);
    }

    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        } else if (p instanceof ActionBar.LayoutParams) {
            return new LayoutParams((ActionBar.LayoutParams) p);
        } else if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        } else {
            return new LayoutParams(p);
        }
    }

    public void setTitleTextColor(@ColorInt int color) {
        mTitleTextColor = color;
        if (tvTitle != null) {
            tvTitle.setTextColor(color);
        }
    }

    public void setBackIcon(@DrawableRes int resId) {
        iconBack = AppCompatResources.getDrawable(getContext(), resId);
        setNavigationIcon(iconBack);
    }

    public void setShowBack(boolean isShow) {
        if (isShow) {
            if (iconBack == null) {
                iconBack = AppCompatResources.getDrawable(getContext(), R.drawable.icon_back);
            }
            setNavigationIcon(iconBack);
        } else {
            setNavigationIcon(null);
        }
    }
}
