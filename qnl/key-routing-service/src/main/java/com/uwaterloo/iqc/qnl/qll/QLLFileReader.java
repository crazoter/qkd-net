package com.uwaterloo.iqc.qnl.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.uwaterloo.iqc.qnl.QNLConfig;

/**
 * QKD Link Layer File Reader
 * It seems that this code is meant to be run by a trusted party (i.e. the KMS).
 * This reads the key from a file that is stored locally at {keyLoc}/{siteId}_{blockIndex}.
 * It processes the document line by line
 */
public class QLLFileReader implements QLLReader {
    // The Simple Logging Facade for Java (SLF4J) serves as an abstraction for various logging frameworks
    // SLF4J allows the end-user to plug in the desired logging framework at deployment time.
    // See http://www.slf4j.org/manual.html for more information.
    private static Logger LOGGER = LoggerFactory.getLogger(QLLFileReader.class);

    private String siteId;
    private AtomicLong atomicQllBlockIndex = new AtomicLong(0);
    private int qllBlockSize;
    private String keyLocalDirectory;
    // This variable is not used:
    private int keyByteSize;

    public QLLFileReader(String id, QNLConfig qnlConfig) {
        this.siteId = id;
        this.qllBlockSize = qnlConfig.getQllBlockSz();
        this.keyLocalDirectory = qnlConfig.getQNLSiteKeyLoc(siteId);
        this.keyByteSize = qnlConfig.getKeyBytesSz();
        LOGGER.info("QLLFileReader.new:" + this);
    }

    /**
     * This is designed to read a text document containing the key on the local directory.
     * It assumes that the keys are stored in blocks (line by line) and reads the value(s) into dst based on the len,
     * accounting for the lines. The keys may be stored in multiple files each of qllBlockSize in bytes.
     * @param dst
     * @param len
     * @param offset
     * @return
     */
    private int readKeyBlock(byte[] dst, int len, long offset) {
        LOGGER.info(this + "-readKeyBlock:len:" + len + ",offset:" + offset);

        int linesRead = 0, linesSkipped = 0, destPos = 0;
        // I'm not sure why there's a isSkipLines haha
        boolean isSkipLines = true;
        try {
            long index = offset;
            long startIndex = index - len;
            while (linesRead < len) {
                long longQllBlockIndex = startIndex / qllBlockSize;
                int qllIndexWithinBlock = (int)startIndex % qllBlockSize;
                // {keyLoc}/{siteId}_{blockIndex}
                String qllFile = keyLocalDirectory + "/" + siteId + "_" + longQllBlockIndex;
                // Warning: file may not exist (caught by IOException)
                BufferedReader reader = new BufferedReader(new FileReader(qllFile));

                if (qllIndexWithinBlock > 0 && isSkipLines) {
                	while (linesSkipped < qllIndexWithinBlock) {
                        String line = reader.readLine();
                        if (line != null)
                        	++linesSkipped;
                        else
                        	break;
                    }
                    isSkipLines = false;
                }

                String line = reader.readLine();
                while (line != null && linesRead < len) {
                	++linesRead;
                    System.arraycopy(line.getBytes(), 0, dst, destPos, line.length());
                    destPos += line.length();
                    line = reader.readLine();
                }
                if (linesRead < len)
                    startIndex = linesRead;
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info(this + "-readKeyBlock:linesRead:" + linesRead);
        return linesRead;
    }

    /**
     * Read into a buffer {len} bytes
     * @param dst The buffer to read the bytes into.
     * @param len The number of bytes to read into dst.
     * @param indexRef "pointer" value that contains the number of bytes read so far.
     *                 This is referred to as the "index" (i.e. the next unread byte).
     *                 However, if lines read is less than the length, this value remains the same; this
     *                 could lead to a bug if developers are not aware of this quirk.
     * @return Number of lineslines read.
     */
    public int read(byte[] dst, int len, AtomicLong indexRef) {
        // WARNING: Potential bug, why are you putting the offset before reading?
        long offset = atomicQllBlockIndex.addAndGet(len);
        int linesRead = readKeyBlock(dst, len, offset);
        if (linesRead == len)
        	indexRef.set(offset);
        else {
        	indexRef.set(-1);
        	atomicQllBlockIndex.addAndGet(-len);
        }
        return linesRead;
    }

    /**
     *
     * @param dst
     * @param len
     * @param offset
     * @return
     */
    public int read(byte[] dst, int len, long offset) {
    	if (offset < 0)
    		return 0;
        return readKeyBlock(dst, len, offset);
    }

    /**
     *
     * @param len This is presumably the same size as qllBlockSz
     * @param indexRef
     */
    public void getNextBlockIndex(int len, AtomicLong indexRef) {
        LOGGER.info(this + ".getNextBlockIndex,len:" + len + ",indexRef:" + indexRef.get());
        // Obtain the block index
        long index = atomicQllBlockIndex.addAndGet(len);
        int blockIndex = (int)index / qllBlockSize;
        // If {keyLoc}/{siteId}_{blockIndex} exists:
        String fileStr = keyLocalDirectory + "/" + siteId + "_" + blockIndex;
        File f = new File(fileStr);
        if (f.exists())
            // Return found index
        	indexRef.set(index);
        else {
            // Else, undo and return -1
        	atomicQllBlockIndex.addAndGet(-len);
        	indexRef.set(-1);
        }
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("QLLFileReader(");
      sb.append(this.siteId);
      sb.append(")");
      sb.append(",qllBlockSz=").append(this.qllBlockSize);
      sb.append(",keyLoc=").append(this.keyLocalDirectory);
      sb.append(",keyByteSz=").append(this.keyByteSize);
      sb.append(",qllBLockIndex=").append(this.atomicQllBlockIndex.get());
      return sb.toString();
    }
}
