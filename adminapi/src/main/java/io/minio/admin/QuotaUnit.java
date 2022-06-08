package io.minio.admin;

/** Bucket Quota QuotaUnit. */
public enum QuotaUnit {

    KB(1024),
    MB(1024 * KB.unit),
    GB(1024 * MB.unit),
    TB(1024 * GB.unit);

    private final long unit;

    QuotaUnit(long unit) {
        this.unit = unit;
    }

    public long toBytes(long size) {
        long totalSize = size * this.unit;
        if (totalSize < 0) {
            throw new IllegalArgumentException("Quota size must be greater than zero.But actual is " + totalSize);
        } else if (totalSize / this.unit != size) {
            throw new IllegalArgumentException("Quota size overflow");
        }
        return totalSize;
    }
}
