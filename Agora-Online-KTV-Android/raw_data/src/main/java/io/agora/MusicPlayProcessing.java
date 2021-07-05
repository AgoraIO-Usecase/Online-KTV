package io.agora;

/**
 * @author chenhengfei(Aslanchen)
 * @date 2021/7/2
 */
public class MusicPlayProcessing {
    static {
        System.loadLibrary("apm-plugin-raw-data");
    }

    public static native void start(int mediaPlayerId);

    public static native void stop();

    public static native void change(int channelIndex);
}
