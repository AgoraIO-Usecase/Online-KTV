package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogChooseSongBinding;
import io.agora.ktv.view.fragment.SongOrdersFragment;
import io.agora.ktv.view.fragment.SongsFragment;

/**
 * 点歌菜单
 *
 * @author chenhengfei@agora.io
 */
public class RoomChooseSongDialog extends BaseBottomSheetDialogFragment<KtvDialogChooseSongBinding> {
    public static final String TAG = RoomChooseSongDialog.class.getSimpleName();

    public static boolean isChorus = false;

    public RoomChooseSongDialog(boolean isChorus) {
        RoomChooseSongDialog.isChorus = isChorus;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(requireDialog().getWindow().getDecorView(), (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mBinding.pager.setPadding(0,0,0, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mBinding.pager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
        mBinding.pager.setAdapter(new FragmentStateAdapter(getChildFragmentManager(), getViewLifecycleOwner().getLifecycle()){

            @Override
            public int getItemCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new SongsFragment();
                }else{
                    return new SongOrdersFragment();
                }
            }

        });
        new TabLayoutMediator(mBinding.tabLayout, mBinding.pager, (tab, position) -> {
            if (position == 0)
                tab.setText(R.string.ktv_room_choose_song);
            else
                tab.setText(R.string.ktv_room_choosed_song);
        }).attach();
    }
}
