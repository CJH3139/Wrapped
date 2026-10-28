package com.wrapped.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HostNormalizerTest {
    @Test
    void hostsNormalize() {
        assertEquals("minehut.com", HostNormalizer.normalize(" Minehut.com. "));
        assertTrue(HostNormalizer.isLocal("192.168.1.5"));
        assertFalse(HostNormalizer.isLocal("mc.hypixel.net"));
    }

    @Test
    void localRanges() {
        assertTrue(HostNormalizer.isLocal("localhost"));
        assertTrue(HostNormalizer.isLocal("127.0.0.1"));
        assertTrue(HostNormalizer.isLocal("10.1.2.3"));
        assertTrue(HostNormalizer.isLocal("172.16.0.1"));
        assertTrue(HostNormalizer.isLocal("172.31.9.9"));
        assertTrue(HostNormalizer.isLocal("::1"));
        assertFalse(HostNormalizer.isLocal("172.15.0.1"));
        assertFalse(HostNormalizer.isLocal("172.32.0.1"));
        assertFalse(HostNormalizer.isLocal("172.example.com"));
    }
}
