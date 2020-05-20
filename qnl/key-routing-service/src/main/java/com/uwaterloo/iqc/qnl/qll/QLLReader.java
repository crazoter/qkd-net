package com.uwaterloo.iqc.qnl.qll;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Interface for the QKD Link Layer File Reader
 */
public interface QLLReader {
    public int read(byte[] dst, int len, AtomicLong index);

    /**
     * Currently not in use in the OpenQKD system.
     * @param len
     * @param index
     */
    public void getNextBlockIndex(int len, AtomicLong index);

    public int read(byte[] dst, int len, long offset);

}
