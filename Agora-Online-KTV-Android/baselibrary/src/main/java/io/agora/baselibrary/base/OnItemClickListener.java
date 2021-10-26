package io.agora.baselibrary.base;

import android.view.View;

import androidx.annotation.NonNull;

public interface OnItemClickListener<T> {
    /**
     * For item data not null
     */
    default void onItemClick(@NonNull T data, View view, int position, long id){

    }

    /**
     * For the null data item
     */
    default void onItemClick(View view, int position, long id) {

    }
}
