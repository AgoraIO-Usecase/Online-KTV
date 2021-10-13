package io.agora.ktv;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.os.Build;
import android.view.View;

public class MyUtil {
    public static void scaleOnTouch(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            StateListAnimator animator = AnimatorInflater.loadStateListAnimator(v.getContext(), R.animator.ktv_scale_on_touch);
            v.setStateListAnimator(animator);
        }
    }
    public static void clearStateListAnimator(View v){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.setStateListAnimator(null);
        }
    }
}
