package io.agora.ktv.view.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.agora.data.manager.UserManager;
import com.agora.data.model.AgoraRoom;
import com.agora.data.model.MusicModel;
import com.agora.data.model.User;
import com.google.android.material.tabs.TabLayoutMediator;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.ktv.R;
import io.agora.ktv.bean.MemberMusicModel;
import io.agora.ktv.databinding.KtvDialogChooseSongBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.view.RoomActivity;
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
    public static boolean isChorus = false;

    public RoomChooseSongDialog(FragmentManager fragmentManager, Lifecycle lifecycle, boolean isChorus) {
        this.fragmentManager = fragmentManager;
        this.lifecycle = lifecycle;
        RoomChooseSongDialog.isChorus = isChorus;
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
        }).attach();
    }

    public static void finishChooseMusic(Context context, MusicModel music){
        AgoraRoom mRoom = RoomManager.Instance(context).getRoom();
        User mUser = UserManager.Instance().getUserLiveData().getValue();
        if (mRoom != null && mUser != null) {
            // Construct a MemberMusicModel
            MemberMusicModel model = new MemberMusicModel(music);
            model.setRoomId(mRoom);
            model.setUserId(mUser.getObjectId());
            model.setId(music.getMusicId());
            model.setMusicId(music.getMusicId());
            model.setType(RoomChooseSongDialog.isChorus? MemberMusicModel.SingType.Chorus : MemberMusicModel.SingType.Single);

            RoomManager.Instance(context).onMusicChanged(model);

            // Chose this dialog
            if(context instanceof RoomActivity)
                ((RoomActivity)context).onBackPressed();
        }
    }
}
