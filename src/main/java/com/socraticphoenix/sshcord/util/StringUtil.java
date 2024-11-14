package com.socraticphoenix.sshcord.util;

import java.util.Set;

public class StringUtil {
    private static final Set<Character> hexChars = Set.of(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f',
            'A', 'B', 'C', 'D', 'E', 'F'
    );

    public static String deEscape(String s) {
        StringBuilder builder = new StringBuilder();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\\') {
                i++;
                if (i < chars.length) {
                    c = chars[i];
                    if (c == 'u') {
                        if (i + 4 < chars.length) {
                            char[] parts = new char[4];
                            boolean valid = true;
                            for (int j = i + 1; j < i + 5; j++) {
                                if (hexChars.contains(chars[j])) {
                                    parts[j - i - 1] = chars[j];
                                } else {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid) {
                                i += 4;
                                int parsed = Integer.parseInt(new String(parts), 16);
                                builder.appendCodePoint(parsed);
                            } else {
                                builder.append(c);
                            }
                        } else {
                            builder.append(c);
                        }
                    } else {
                        builder.append(switch (c) {
                            case 't' -> '\t';
                            case 'b' -> '\b';
                            case 'n' -> '\n';
                            case 'r' -> '\r';
                            case 'f' -> '\f';
                            default -> c;
                        });
                    }
                } else {
                    builder.append(c);
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
