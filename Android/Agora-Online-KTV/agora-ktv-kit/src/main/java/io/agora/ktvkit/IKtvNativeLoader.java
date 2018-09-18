package io.agora.ktvkit;

public interface IKtvNativeLoader {
    void loadLibrary(String libName) throws UnsatisfiedLinkError,
            SecurityException;
}
