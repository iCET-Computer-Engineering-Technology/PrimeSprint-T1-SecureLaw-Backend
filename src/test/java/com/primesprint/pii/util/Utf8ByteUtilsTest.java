package com.primesprint.pii.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Utf8ByteUtilsTest {

    @Test
    void sliceByUtf8ByteOffsets_ascii_ok() {
        String s = "Client John";
        assertEquals("Client", Utf8ByteUtils.sliceByUtf8ByteOffsets(s, 0, 6));
        assertEquals("John", Utf8ByteUtils.sliceByUtf8ByteOffsets(s, 7, 11));
    }

    @Test
    void sliceByUtf8ByteOffsets_utf8_ok() {
        // 'é' is 2 bytes in UTF-8
        String s = "café";
        assertEquals(5, Utf8ByteUtils.utf8Length(s));
        assertEquals("é", Utf8ByteUtils.sliceByUtf8ByteOffsets(s, 3, 5));
    }
}

