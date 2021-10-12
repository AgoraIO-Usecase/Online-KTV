package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogChooseSongBinding;
import io.agora.ktv.view.SongsFragment;

/**
 * 点歌菜单
 *
 * @author chenhengfei@agora.io
 */
public class RoomChooseSongDialog extends BaseBottomSheetDialogFragment<KtvDialogChooseSongBinding> {
    public static final String TAG = RoomChooseSongDialog.class.getSimpleName();

    private final FragmentManager fragmentManager;
    private final Lifecycle lifecycle;
    private boolean isChorus;

    public RoomChooseSongDialog(FragmentManager fragmentManager, Lifecycle lifecycle, boolean isChorus) {
        this.fragmentManager = fragmentManager;
        this.lifecycle = lifecycle;
        this.isChorus = isChorus;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.pager.setAdapter(new FragmentStateAdapter(fragmentManager, lifecycle){

            @Override
            public int getItemCount() {
                return 1;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new SongsFragment();
            }
        });
        new TabLayoutMediator(mBinding.tabLayout, mBinding.pager, (tab, position) -> {
            if (position == 0)
                tab.setText(R.string.ktv_room_choose_song);
            else
                tab.setText(R.string.ktv_room_choosed_song);
        });
//        mBinding.pager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager(), FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
//
//            @Override
//            public int getCount() {
//                return 1;
//            }
//
//            @NonNull
//            @Override
//            public Fragment getItem(int position) {
//                return mSongsFragment;
//            }
//
//            @Override
//            public CharSequence getPageTitle(int position) {
//                if (position == 0) {
//                    return getString(R.string.ktv_room_choose_song);
//                } else {
//                    return getString(R.string.ktv_room_choosed_song);
//                }
//            }
//        });
    }
}
