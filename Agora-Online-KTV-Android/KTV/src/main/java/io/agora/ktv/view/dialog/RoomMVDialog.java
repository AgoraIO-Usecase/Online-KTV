package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.agora.data.ExampleData;
import com.agora.data.model.AgoraRoom;

import io.agora.baselibrary.base.BaseBottomSheetDialogFragment;
import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.baselibrary.base.OnItemClickListener;
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
    private static final String TAG = RoomMVDialog.class.getSimpleName();

    private static final String TAG_MV_INDEX = "mvIndex";

    private BaseRecyclerViewAdapter<KtvItemMvBinding, Integer, MVHolder> mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    public void initView() {
        mAdapter = new BaseRecyclerViewAdapter<>(ExampleData.exampleBackgrounds, this, MVHolder.class);
        if(getArguments()!=null){
            mAdapter.selectedIndex = getArguments().getInt(TAG_MV_INDEX);
        }else mAdapter.selectedIndex = 0;

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
    public void onItemClick(@NonNull Integer data, View view, int position, long id) {
        AgoraRoom mRoom = RoomManager.Instance(requireContext()).getRoom();
        if (mRoom == null) {
            dismiss();
            return;
        }
        int formerIndex = mAdapter.selectedIndex;
        mAdapter.selectedIndex = position;
        mAdapter.notifyItemChanged(formerIndex);
        mAdapter.notifyItemChanged(position);
        ExampleData.updateCoverImage(position);
    }

    @Override
    public void onItemClick(View view, int position, long id) {

    }
}
