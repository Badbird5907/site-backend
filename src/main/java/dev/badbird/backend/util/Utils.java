package dev.badbird.backend.util;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Component
public class Utils {
    public static boolean isAlphaNumeric(String s, char... exclude) {
        if (s.isEmpty()) {
            return false;
        }
        final int sz = s.length();
        List<Character> excludeList = List.of(ArrayUtils.toObject(exclude));
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(s.charAt(i)) && !excludeList.contains(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String getRandomString(int len) {
        String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(AB.charAt(new Random().nextInt(AB.length())));
        }
        return sb.toString();
    }

    private static final Pattern REGEX_ESCAPE = Pattern.compile("([\\\\\\[\\]\\{\\}\\(\\)\\*\\+\\?\\|\\^\\$\\.])");

    public static String escapeRegex(String str) {
        return REGEX_ESCAPE.matcher(str).replaceAll("\\\\$1");
    }
}
