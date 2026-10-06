package info.t4w.vp.database;

/**
 * Native Next database bridge required by libnext.so JNI_OnLoad dynamic registration.
 */
public class Next {
    public static native String v0(String input);
    public static native boolean v1(String input, int type);
}
