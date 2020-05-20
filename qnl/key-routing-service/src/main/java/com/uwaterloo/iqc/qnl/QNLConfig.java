package com.uwaterloo.iqc.qnl;

import java.util.Map;

public class QNLConfig {

    private String base;
    private String routeConfigLoc;
    private String qnlSiteKeyLoc;
    private String siteId;
    private int port;
    private int keyBytesSz;
    private int keyBlockSz;
    private int qllBlockSz;
    private int headerSz;
    private String kmsIP;
    private int kmsPort;
    public Map<String, String> OTPConfig;
    public static final String OTP_KEYBLOCKSIZE = "keyBlockSz";
    public static final String KMS = "kms";

    public String getRouteConfigLoc() {
        return "D:\\Study\\Y2 Summer\\SpeQtral\\OpenQKD\\qnl\\conf\\routes.json";
        //return System.getProperty("user.home") + "/" + base + "/" + routeConfigLoc;
    }

    public String getQNLSiteKeyLoc(String siteId) {
        return System.getProperty("user.home") + "/" + base + "/"  +
               "/" + qnlSiteKeyLoc + "/" + siteId;
    }

    public String getOTPKeyLoc(String siteId) {
        return System.getProperty("user.home") + "/" + base + "/" +
               "/" + OTPConfig.get("keyLoc") + "/" + siteId;
    }

    public String getBase() {
        return base;
    }

    public String getQnlSiteKeyLoc() {
        return qnlSiteKeyLoc;
    }


    public int getKeyBytesSz() {
        return keyBytesSz;
    }

    public int getKeyBlockSz() {
        return keyBlockSz;
    }

    public int getPort() {
        return port;
    }

    public int getQllBlockSz() {
        return qllBlockSz;
    }

    public int getHeaderSz() {
        return headerSz;
    }

    public String getSiteId() {
        return siteId;
    }

    public int getKmsPort() {
        return kmsPort;
    }

    public String getKmsIP() {
        return kmsIP;
    }

    public int getOTPKeyBlockSz() {
        return Integer.valueOf(OTPConfig.get(OTP_KEYBLOCKSIZE)).intValue();
    }

}