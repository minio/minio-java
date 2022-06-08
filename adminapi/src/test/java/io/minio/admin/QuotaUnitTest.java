package io.minio.admin;

import org.junit.Test;

public class QuotaUnitTest {
    @Test(expected = IllegalArgumentException.class)
    public void testBytesOverflow() {
        QuotaUnit.GB.toBytes(Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBytesLessThanZero() {
        QuotaUnit.KB.toBytes(-1);
    }
}