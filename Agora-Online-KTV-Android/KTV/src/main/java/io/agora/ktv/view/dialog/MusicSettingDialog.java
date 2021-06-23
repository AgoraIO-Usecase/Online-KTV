package io.agora.ktv.view.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import io.agora.baselibrary.base.DataBindBaseDialog;
import io.agora.ktv.R;
import io.agora.ktv.databinding.KtvDialogMusicSettingBinding;

/**
 * 歌曲菜单
 *
 * @author chenhengfei@agora.io
 */
public class MusicSettingDialog extends DataBindBaseDialog<KtvDialogMusicSettingBinding> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = MusicSettingDialog.class.getSimpleName();

    private static final String TAG_EAR = "ear";
    private static final String TAG_MIC_VOL = "mic_vol";
    private static final String TAG_MUSIC_VOL = "music_vol";

    private boolean isEar;
    private int volMic;
    private int volMusic;

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
        isEar = bundle.getBoolean(TAG_EAR);
        volMic = bundle.getInt(TAG_MIC_VOL);
        volMusic = bundle.getInt(TAG_MUSIC_VOL);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ktv_dialog_music_setting;
    }

    @Override
    public void iniView() {

    }

    @Override
    public void iniListener() {
    }

    @Override
    public void iniData() {
        mDataBinding.switchEar.setChecked(isEar);
        mDataBinding.sbVol1.setProgress(volMic);
        mDataBinding.sbVol2.setProgress(volMusic);

        mDataBinding.switchEar.setOnCheckedChangeListener(this);
        mDataBinding.sbVol1.setOnSeekBarChangeListener(this);
        mDataBinding.sbVol2.setOnSeekBarChangeListener(this);
    }

    public void show(@NonNull FragmentManager manager, boolean isEar, int volMic, int volMusic, Callback mCallback) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(TAG_EAR, isEar);
        bundle.putInt(TAG_MIC_VOL, volMic);
        bundle.putInt(TAG_MUSIC_VOL, volMusic);
        setArguments(bundle);
        this.mCallback = mCallback;
        super.show(manager, TAG);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        isEar = isChecked;
        this.mCallback.onEarChanged(isEar);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mDataBinding.sbVol1) {
            volMic = progress;
            this.mCallback.onMicVolChanged(progress);
        } else if (seekBar == mDataBinding.sbVol2) {
            volMusic = progress;
            this.mCallback.onMusicVolChanged(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private Callback mCallback;

    public interface Callback {
        void onEarChanged(boolean isEar);

        void onMicVolChanged(int vol);

        void onMusicVolChanged(int vol);
    }
}
