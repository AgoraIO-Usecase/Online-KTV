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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvDialogChooseSongBinding;
import io.agora.ktv.view.SongOrdersFragment;
import io.agora.ktv.view.SongsFragment;

/**
 * 点歌菜单
 *
 * @author chenhengfei@agora.io
 */
public class RoomChooseSongDialog extends DataBindBaseDialog<KtvDialogChooseSongBinding> implements ViewPager.OnPageChangeListener {

    private static final String TAG = RoomChooseSongDialog.class.getSimpleName();

    public static MemberMusicModel.SingType mSingType = null;

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
        mDataBinding.pager.addOnPageChangeListener(this);
    }

    private SongsFragment mSongsFragment = SongsFragment.newInstance();
    private SongOrdersFragment mSongOrdersFragment = SongOrdersFragment.newInstance();

    @Override
    public void iniData() {
        mDataBinding.pager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {

            @Override
            public int getCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    return mSongsFragment;
                } else {
                    return mSongOrdersFragment;
                }
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.ktv_room_choose_song);
                } else {
                    return getString(R.string.ktv_room_choosed_song);
                }
            }
        });

        mDataBinding.tabLayout.setupWithViewPager(mDataBinding.pager);
    }

    public void show(@NonNull FragmentManager manager) {
        super.show(manager, TAG);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position == 1) {
            mSongOrdersFragment.iniData();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataBinding.pager.removeOnPageChangeListener(this);
    }
}
