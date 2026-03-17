package com.primesprint.util;

import java.nio.charset.StandardCharsets;

public final class Utf8ByteUtils {
    private Utf8ByteUtils() {}

    public static String sliceByUtf8ByteOffsets(String source, int startByteInclusive, int endByteExclusive) {
        if (source == null) return null;
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        if (startByteInclusive < 0 || endByteExclusive < 0 || startByteInclusive >= endByteExclusive || endByteExclusive > bytes.length) {
            throw new IllegalArgumentException("Invalid UTF-8 byte offsets start=" + startByteInclusive + ", end=" + endByteExclusive + ", len=" + bytes.length);
        }
        return new String(bytes, startByteInclusive, endByteExclusive - startByteInclusive, StandardCharsets.UTF_8);
    }

    public static int utf8Length(String source) {
        if (source == null) return 0;
        return source.getBytes(StandardCharsets.UTF_8).length;
    }

    public static int indexOfUtf8Bytes(String source, String needle) {
        if (source == null || needle == null) return -1;
        byte[] haystack = source.getBytes(StandardCharsets.UTF_8);
        byte[] target = needle.getBytes(StandardCharsets.UTF_8);
        if (target.length == 0) return 0;

        outer:
        for (int i = 0; i <= haystack.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (haystack[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}

