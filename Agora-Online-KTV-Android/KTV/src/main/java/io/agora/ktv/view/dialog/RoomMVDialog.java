package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.agora.data.ExampleData;
import com.agora.data.model.AgoraRoom;
import com.agora.data.provider.AgoraObject;
import com.agora.data.sync.AgoraException;
import com.agora.data.sync.SyncManager;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
import io.agora.baselibrary.util.ToastUtil;
import io.agora.ktv.adapter.MVHolder;
import io.agora.ktv.databinding.KtvDialogMvBinding;
import io.agora.ktv.databinding.KtvItemMvBinding;
import io.agora.ktv.manager.RoomManager;
import io.agora.ktv.widget.DividerDecoration;

/**
 * 房间MV菜单
 *
 * @author chenhengfei@agora.io
 */
public class RoomMVDialog extends BaseBottomSheetDialogFragment<KtvDialogMvBinding> implements OnItemClickListener<Integer> {
    public static final String TAG = RoomMVDialog.class.getSimpleName();

    private static final String TAG_MV_INDEX = "mvIndex";

    private int index = 0;
    private BaseRecyclerViewAdapter<KtvItemMvBinding, Integer, MVHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }
    private void initView() {
        mAdapter =new BaseRecyclerViewAdapter<>(ExampleData.exampleBackgrounds, this, MVHolder.class);
        mAdapter.selectedIndex = index;

        mBinding.rvList.setAdapter(mAdapter);
        mBinding.rvList.addItemDecoration(new DividerDecoration(3));
    }

    public void show(@NonNull FragmentManager manager, int index) {
        Bundle mBundle = new Bundle();
        mBundle.putInt(TAG_MV_INDEX, index);
        setArguments(mBundle);
        super.show(manager, TAG);
    }

    @Override
    public void onItemClick(@NonNull Integer data, View view, int position, long viewType) {
        AgoraRoom mRoom = RoomManager.Instance(requireContext()).getRoom();
        if (mRoom == null) {
            dismiss();
            return;
        }

        mAdapter.selectedIndex = position;
        SyncManager.Instance()
                .getRoom(mRoom.getId())
                .update(AgoraRoom.COLUMN_MV, String.valueOf(position + 1), new SyncManager.DataItemCallback() {
                    @Override
                    public void onSuccess(AgoraObject result) {

                    }

                    @Override
                    public void onFail(AgoraException exception) {
                        ToastUtil.toastShort(requireContext(), exception.getMessage());
                    }
                });
    }
}
