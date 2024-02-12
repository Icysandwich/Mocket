package mocket.instrument;

/**
 * Adapted from Apache harmony's String.java, under the
 * Apache License v2.
 */
public class StringUtils {

    private StringUtils() {
        // Prevents this class from being instantiated
    }

    private static boolean _startsWith(String thisStr, String string) {
        if(thisStr.length() < string.length()) {
            return false;
        }
        for(int i = 0; i < string.length(); ++i) {
            if(thisStr.charAt(i) != string.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWith(String str, String prefix) {
        return _startsWith(str, prefix);
    }

    public static boolean contains(String thisStr, String string) {
        return thisStr.contains(string.subSequence(0, string.length() - 1));
    }
}
