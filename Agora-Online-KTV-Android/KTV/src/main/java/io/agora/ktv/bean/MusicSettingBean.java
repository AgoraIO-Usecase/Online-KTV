package io.agora.ktv.bean;

import io.agora.ktv.view.dialog.MusicSettingDialog;

public class MusicSettingBean {
    private final MusicSettingDialog.Callback mCallback;
    private boolean isEar;
    private int volMic;
    private int volMusic;
    private int effect;
    private int toneValue;

    public MusicSettingBean(boolean isEar, int volMic, int volMusic,int toneValue, MusicSettingDialog.Callback mCallback) {
        this.isEar = isEar;
        this.volMic = volMic;
        this.volMusic = volMusic;
        this.mCallback = mCallback;
        this.toneValue = toneValue;
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

    public int getEffect() {
        return effect;
    }

    public void setEffect(int effect) {
        this.effect = effect;
        this.mCallback.onEffectChanged(effect);
    }

    public int getToneValue() {
        return toneValue;
    }

    public void setToneValue(int newToneValue) {
        this.toneValue = newToneValue;
        this.mCallback.onToneChanged(newToneValue);
    }
}
