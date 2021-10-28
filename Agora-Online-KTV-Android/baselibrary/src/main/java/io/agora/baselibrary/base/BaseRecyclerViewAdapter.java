package io.agora.baselibrary.base;

import static io.agora.baselibrary.util.KTVUtil.getGenericClass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import io.agora.baselibrary.util.KTVUtil;

/**
 * 基础RecyclerView adapter
 *
 * @author chenhengfei@agora.io
 */
@Keep
public class BaseRecyclerViewAdapter<B extends ViewBinding, T,H extends BaseRecyclerViewAdapter.BaseViewHolder<B,T>> extends RecyclerView.Adapter<H> {

    public List<T> dataList;
    private OnItemClickListener<T> mOnItemClickListener;
    public int selectedIndex = -1;

    private Class<B> bindingClass;
    private final Class<H> viewHolderClass;

    public BaseRecyclerViewAdapter(@Nullable List<T> dataList, Class<H> viewHolderClass) {
        this(dataList, null, viewHolderClass);
    }


    public BaseRecyclerViewAdapter(@Nullable List<T> dataList, @Nullable OnItemClickListener<T> listener, Class<H> viewHolderClass) {
        this.viewHolderClass = viewHolderClass;
        if (dataList == null) {
            this.dataList = new ArrayList<>();
        } else {
            this.dataList = new ArrayList<>(dataList);
        }

        if (listener != null) {
            this.mOnItemClickListener = (OnItemClickListener<T>) listener;
        }
    }

    public H createHolder(ViewBinding mBinding) {
        ensureBindingClass();
        try {
            Constructor<H> constructor = viewHolderClass.getConstructor(bindingClass);
            return constructor.newInstance(mBinding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @NonNull
    @Override
    public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        B mBinding = getViewBindingByReflect(LayoutInflater.from(parent.getContext()), parent);
        H holder = createHolder(mBinding);
        holder.mListener = (BaseViewHolder.OnTempItemClickListener) (view, position, id) -> {
            if (BaseRecyclerViewAdapter.this.mOnItemClickListener != null) {
                T data = getItemData(position);
                if (data == null) {
                    BaseRecyclerViewAdapter.this.mOnItemClickListener.onItemClick(view, position, id);
                } else {
                    BaseRecyclerViewAdapter.this.mOnItemClickListener.onItemClick(data, view, position, id);
                }

            }
        };
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull H holder, int position) {
        T data = dataList.get(position);
        holder.binding(data, selectedIndex);
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    @Nullable
    public T getItemData(int position) {
        if (dataList == null) {
            return null;
        }

        if (position < 0 || dataList.size() <= position) {
            return null;
        }

        return dataList.get(position);
    }

    public B getViewBindingByReflect(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        ensureBindingClass();
        try {
            return KTVUtil.getViewBinding(bindingClass,inflater,container);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //<editor-fold desc="CURD">
    public boolean contains(@NonNull T data) {
        if (dataList == null) {
            return false;
        }
        return dataList.contains(data);
    }

    public int indexOf(@NonNull T data) {
        if (dataList == null) {
            return -1;
        }
        return dataList.indexOf(data);
    }

    public void setDataList(@NonNull List<T> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    public void addItem(@NonNull T data) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        int index = dataList.indexOf(data);
        if (index < 0) {
            dataList.add(data);
            notifyItemInserted(dataList.size() - 1);
        } else {
            dataList.set(index, data);
            notifyItemChanged(index);
        }
    }

    public void addItem(@NonNull T data, int index) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        int indexTemp = dataList.indexOf(data);
        if (indexTemp < 0) {
            dataList.add(index, data);
            notifyItemRangeChanged(index, dataList.size() - index);
        } else {
            dataList.set(index, data);
            notifyItemChanged(index);
        }
    }

    public void update(int index, @NonNull T data) {
        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        dataList.set(index, data);
        notifyItemChanged(index);
    }

    public void clear() {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        dataList.clear();
        notifyDataSetChanged();
    }

    public void deleteItem(@Size(min = 0) int posion) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        if (0 <= posion && posion < dataList.size()) {
            dataList.remove(posion);
            notifyItemRemoved(posion);
        }
    }

    public void deleteItem(@NonNull T data) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        int index = dataList.indexOf(data);
        if (0 <= index && index < dataList.size()) {
            dataList.remove(data);
            notifyItemRemoved(index);
        }
    }
    //</editor-fold>
    public void ensureBindingClass(){
        if (bindingClass == null)
            bindingClass = getGenericClass(viewHolderClass, 0);
    }
    @Keep
    public static abstract class BaseViewHolder<B extends ViewBinding, T> extends RecyclerView.ViewHolder {
        public OnTempItemClickListener mListener;
        public final B mBinding;

        public BaseViewHolder(@NonNull B mBinding) {
            super(mBinding.getRoot());
            this.mBinding = mBinding;
            mBinding.getRoot().setOnClickListener(this::onItemClick);
        }

        protected void onItemClick(View view) {
            if (mListener != null) {
                final int position = getAdapterPosition();
                mListener.onItemClick(view, position, getItemId());
            }
        }

        interface OnTempItemClickListener {
            void onItemClick(View view, int position, long id);
        }
        public abstract void binding(@Nullable T data, int selectedIndex);
    }
}
