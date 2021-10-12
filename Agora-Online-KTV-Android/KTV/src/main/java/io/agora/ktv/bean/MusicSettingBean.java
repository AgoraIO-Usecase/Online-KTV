package io.agora.ktv.bean;

import io.agora.ktv.view.dialog.MusicSettingDialog;

public class MusicSettingBean {
    private final MusicSettingDialog.Callback mCallback;
    private boolean isEar;
    private int volMic;
    private int volMusic;

    public MusicSettingBean(boolean isEar, int volMic, int volMusic, MusicSettingDialog.Callback mCallback) {
        this.isEar = isEar;
        this.volMic = volMic;
        this.volMusic = volMusic;
        this.mCallback = mCallback;
    }

    public MusicSettingDialog.Callback getCallback() {
        return mCallback;
    }

    public boolean isEar() {
        return isEar;
    }

    public void setEar(boolean ear) {
        isEar = ear;
        mCallback.onEarChanged(ear);
    }

    public int getVolMic() {
        return volMic;
    }

    public void setVolMic(int volMic) {
        this.volMic = volMic;
        this.mCallback.onMicVolChanged(volMic);
    }

    public int getVolMusic() {
        return volMusic;
    }

    public void setVolMusic(int volMusic) {
        this.volMusic = volMusic;
        this.mCallback.onMusicVolChanged(volMusic);
    }
}
