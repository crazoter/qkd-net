package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Hex;

import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.qkd.qnl.utils.QNLUtils;

public class OTPKey {
    private static Logger LOGGER = LoggerFactory.getLogger(OTPKey.class);

    private QNLConfiguration qnlConfig;
    private String id;
    private byte[] otpKey;

    public static final String OTPKEYNAME = "otpkey";

    public OTPKey(QNLConfiguration qnlConfig, String id) {
        this.qnlConfig = qnlConfig;
        this.id = id;
        LOGGER.info("OTPKey.new:id=" + id);
        createKey();
    }

    public void otp(byte[] data) throws Exception {
        int i = 0;
        for (byte b : otpKey)
            data[i] = (byte)(b ^ data[i++]);
    }

    private void createKey() {
        QNLConfig config = qnlConfig.getConfig();
        byte[] hex =  new byte[config.getKeyBlockSz()*config.getKeyBytesSz()*2];
        // If the OTPKeyLoc directory doesn't exist, create it
        try {
        	File otpF = new File(config.getOTPKeyLoc(id));
        	if (!otpF.exists()) {
        		otpF.mkdirs();
        	}
        } catch(Exception e) {}

        String otpFile = config.getOTPKeyLoc(id) + "/" + OTPKEYNAME; //OTPKEYNAME = "otpkey" (constant)
        File f = new File(otpFile);
        // If OTP File doesn't exist, read one block (key?) from the QLLReader into the hex buffer
        //     and also into the OTP file
        // Otherwise, read from the OTP file into the hex buffer
        if(!f.exists()) {
            QLLReader QLLRdr = qnlConfig.getQLLReader(id);
            AtomicLong ref = new AtomicLong(0);
            QLLRdr.getNextBlockIndex(config.getKeyBlockSz(), ref);
            QLLRdr.read(hex, config.getKeyBlockSz(), ref.get());
            QNLUtils.writeKeys(hex, otpFile, config.getKeyBlockSz());
        } else {
            QNLUtils.readKeys(hex, otpFile, config.getKeyBlockSz());
        }

        // Convert hexadecimal strings into a byte array of the same value otpKey
        try {
            otpKey = new Hex().decode(hex);
        } catch(Exception e) {}
    }
}
