package info.t4w.vp.view;

/**
 * Native MiniContainer bridge required by libnext.so JNI_OnLoad dynamic registration.
 */
public class MiniContainer {
    public static native byte[] x0(byte[] input, String key);
    public static native byte[] x1(byte[] input);
}
