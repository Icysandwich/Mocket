package mocket.instrument;

/**
 * Adapted from Phosphor's Instrumenter.java, under the MIT License.
 */

public class Instrumenter {

    public static boolean isIgnoredClass(String owner) {
        return StringUtils.startsWith(owner, "java/")
                || StringUtils.startsWith(owner, "sun/");
    }

    public static boolean isPrimitiveTypes(String descriptor) {
        return descriptor.equals("B") || descriptor.equals("S")
                || descriptor.equals("I")
                || descriptor.equals("J")
                || descriptor.equals("F")
                || descriptor.equals("D")
                || descriptor.equals("C")
                || descriptor.equals("Z");
    }

    public static String getBoxingTypeForPrimitiveType(String descriptor) {
        switch (descriptor) {
            case "B":
                return "java/lang/Byte";
            case "S":
                return "java/lang/Short";
            case "I":
                return "java/lang/Integer";
            case "J":
                return "java/lang/Long";
            case "F":
                return "java/lang/Float";
            case "D":
                return "java/lang/Double";
            case "C":
                return "java/lang/Char";
            case "Z":
                return "java/lang/Boolean";
            default:
                return descriptor;
        }
    }
}
