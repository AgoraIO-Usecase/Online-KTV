package io.agora.ktv.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.agora.baselibrary.base.BaseRecyclerViewAdapter;
import io.agora.ktv.R;
import io.agora.ktv.bean.MVModel;
import io.agora.ktv.databinding.KtvItemRoomSpeakerBinding;

/**
 * MVModel List
 *
 * @author chenhengfei@agora.io
 */
public class MVAdapter extends BaseRecyclerViewAdapter<MVModel, MVAdapter.ViewHolder> {

    public MVAdapter(@Nullable List<MVModel> datas, @Nullable Object listener) {
        super(datas, listener);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_item_mv;
    }

    @Override
    public ViewHolder createHolder(View view, int viewType) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MVModel item = getItemData(position);
        if (item == null) {
            return;
        }

//        holder.mDataBinding.tvName.setText(item.getName());
    }

    class ViewHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomSpeakerBinding> {

        public ViewHolder(View view) {
            super(view);
        }
    }
}
