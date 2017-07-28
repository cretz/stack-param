package stackparam;

import java.util.Arrays;
import java.lang.reflect.Method;

public class StackParamNative {

    /**
     * Maximum number of chars a single param can take before it is chopped
     * and has an ellipsis appended.
     */
    public static int MAX_PARAM_STR_LEN = 50;

    public static Method DEFAULT_TO_STRING;
    static {
        try {
            DEFAULT_TO_STRING = Object.class.getMethod("toString");
        } catch (Exception e) {
            //can't happen
        }
    }

    /**
     * Returns the stack params of the given thread for the given depth. It is
     * returned with closest depth first.
     *
     * Each returned sub array (representing a single depth) has params
     * including "this" as the first param for non-static methods. Each param
     * takes 3 values in the array: the string name, the string JVM type
     * signature, and the actual object. All primitives are boxed.
     *
     * In cases where the param cannot be obtained (i.e. non-"this" for native
     * methods), the string "<unknown>" becomes the value regardless of the
     * type's signature.
     *
     * @param thread The thread to get params for
     * @param maxDepth The maximum depth to go to
     * @return Array where each value represents params for a frame. Each param
     *         takes 3 spots in the sub-array for name, type, and value.
     * @throws NullPointerException If thread is null
     * @throws IllegalArgumentException If maxDepth is negative
     * @throws RuntimeException Any internal error we were not prepared for
     */
    public static native Object[][] loadStackParams(Thread thread, int maxDepth);

    /**
     * Appends params string, e.g. "[foo=bar, baz=null]" to the given frame
     * string. Any exceptions during string building are trapped.
     *
     * @param frameString The string to append to
     * @param params The array for params. Must be multiple of 3 as returned by
     *               loadStackParams.
     * @return The resulting string
     */
    public static String appendParamsToFrameString(String frameString, Object[] params) {
        try {
            if (params == null) return frameString;
            StringBuilder ret = new StringBuilder(frameString);
            ret.append(" [");
            for (int i = 0; i < params.length / 3; i++) {
                if (i > 0) ret.append(", ");
                ret.append((String) params[i * 3]).append("=");
                String param;
                try {
                    param = paramValToString(params[(i * 3) + 2]);
                } catch (Exception e) {
                    ret.append("toString err: ").append(e.toString());
                    continue;
                }
                if (param.length() <= MAX_PARAM_STR_LEN) ret.append(param);
                else ret.append(param, 0, MAX_PARAM_STR_LEN).append("...");
            }
            return ret.append("]").toString().replace("\n", "\\n");
        } catch (Exception e) {
            return frameString + "[failed getting params: " + e + "]";
        }
    }

    private static String paramValToString(Object paramVal) {
        if (paramVal == null) {
            return "null";
        }

        if (paramVal.getClass().isArray()) {
            if (paramVal instanceof boolean[]) return Arrays.toString((boolean[]) paramVal);
            else if (paramVal instanceof byte[]) return Arrays.toString((byte[]) paramVal);
            else if (paramVal instanceof char[]) return Arrays.toString((char[]) paramVal);
            else if (paramVal instanceof short[]) return Arrays.toString((short[]) paramVal);
            else if (paramVal instanceof int[]) return Arrays.toString((int[]) paramVal);
            else if (paramVal instanceof long[]) return Arrays.toString((long[]) paramVal);
            else if (paramVal instanceof float[]) return Arrays.toString((float[]) paramVal);
            else if (paramVal instanceof double[]) return Arrays.toString((double[]) paramVal);
            else return Arrays.toString((Object[]) paramVal);
        }

        // Collapse package names to first letters when using default toString for objects
        try {
            Method toStringMethod = paramVal.getClass().getMethod("toString");
            if (DEFAULT_TO_STRING.equals(toStringMethod)) {
                String tmp = paramVal.toString();
                String[] split = tmp.split("\\.");
                StringBuilder out = new StringBuilder();
                for(int i=0; i<split.length - 1; i++) {
                    String s = split[i];
                    out.append(s.charAt(0)).append(".");
                }
                out.append(split[split.length-1]);
                return out.toString();
            }
        } catch(Exception e) {
            //all objects have toString methods, so can't happen
            throw new RuntimeException("Can't happen", e);
        }
        return String.valueOf(paramVal);
    }
}
