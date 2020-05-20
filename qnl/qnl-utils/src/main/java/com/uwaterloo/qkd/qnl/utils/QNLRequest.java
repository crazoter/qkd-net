package com.uwaterloo.qkd.qnl.utils;

import java.util.Formatter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class QNLRequest {

    private String srcSiteId;
    private int srcSiteIdLen;
    private String dstSiteId;
    private int dstSiteIdLen;
    private short opId;
    private short respOpId;
    private long keyBlockIndex;
    private int kpBlockBytesSz;
    private int frameSize;
    private ByteBuf payBuf;
    private boolean payLoadMode = false;
    private String uuid;

    public QNLRequest(int kpBlockByteSz) {
        this.kpBlockBytesSz = kpBlockByteSz;
        payBuf = Unpooled.buffer(kpBlockByteSz + 128);
        frameSize = 0;
    }

    public void setOpId(short op) {
        // Warning: Should add check before adding frameSize
        frameSize += Short.BYTES;
        opId = op;
    }

    public short getOpId() {
        return opId;
    }

    public void setRespOpId(short op) {
        // Warning: Should add check before adding frameSize
        frameSize += Short.BYTES;
        respOpId = op;
    }

    public short getRespOpId() {
        return respOpId;
    }

    public void setUUID(String id) {
        // Warning: Should add check before adding frameSize
        frameSize += id.length() + 2;
        uuid = id;
    }

    public String getUUID() {
        return uuid;
    }

    public void setKeyBlockIndex(long index) {
        // Warning: Should add check before adding frameSize
        frameSize += Long.BYTES;
        keyBlockIndex = index;
    }

    public long getKeyBlockIndex() {
        return keyBlockIndex;
    }

    public void setSiteIds(String src, String dst) {
        // Warning: Should add check before adding frameSize
        srcSiteIdLen = src.length();
        frameSize += srcSiteIdLen + 2;
        srcSiteId = src;

        dstSiteIdLen = dst.length();
        frameSize += srcSiteIdLen + 2;
        dstSiteId = dst;
    }

    public String getSrcSiteId() {
        return srcSiteId;
    }

    public String getDstSiteId() {
        return dstSiteId;
    }

    /**
     * Given this QNLRequest object, write it into the byte buffer param.
     * @param out
     */
    public void encode(ByteBuf out) {
        switch (opId) {
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            frameSize += payBuf.readableBytes();
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            frameSize += payBuf.readableBytes();
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            break;
        }
        // Warning: Should verify that the values are set.
        out.writeInt(frameSize);
        out.writeShort(opId);
        out.writeShort(srcSiteIdLen);
        out.writeBytes(srcSiteId.getBytes(), 0, srcSiteIdLen);
        out.writeShort(dstSiteIdLen);
        out.writeBytes(dstSiteId.getBytes(), 0, dstSiteIdLen);

        switch (opId) {
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeShort(respOpId);
            out.writeBytes(payBuf);
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            out.writeBytes(payBuf);
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            out.writeShort(uuid.length());
            out.writeBytes(uuid.getBytes(), 0, uuid.length());
            out.writeLong(keyBlockIndex);
            break;
        }
    }

    public void setPayLoad(byte[] payLoad) {
        payBuf.writeBytes(payLoad);
    }

    public ByteBuf getPayLoad() {
        return payBuf;
    }

    /**
     * Given a byte buffer representing a frame, read from it into this QNLRequest
     * @param frame
     * @return
     */
    public boolean decode(ByteBuf frame) {
        int m =  frame.readableBytes();
        short uuidLen;
        byte [] id;

        if (!this.payLoadMode) {
            frameSize = frame.readInt();
            this.opId = frame.readShort();
            frameSize -= Short.BYTES;

            this.srcSiteIdLen = frame.readShort();
            frameSize -= Short.BYTES;
            byte [] src = new byte[srcSiteIdLen];
            frame.readBytes(src);
            this.srcSiteId = new String(src);
            frameSize -= this.srcSiteIdLen;

            this.dstSiteIdLen = frame.readShort();
            frameSize -= Short.BYTES;
            byte [] dst = new byte[dstSiteIdLen];
            frame.readBytes(dst);
            this.dstSiteId = new String(dst);
            frameSize -= this.dstSiteIdLen;

            switch (opId) {
            case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_KP_BLOCK_INDEX:
                break;
            case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSize -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSize -= uuidLen;

                this.keyBlockIndex = frame.readLong();
                frameSize -= Long.BYTES;

                frameSize -= frame.readableBytes();
                frame.readBytes(payBuf, frame.readableBytes());
                payLoadMode = !(frameSize == 0);
                break;
            case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_KP_BLOCK_INDEX:
                uuidLen = frame.readShort();
                frameSize -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSize -= uuidLen;

                frameSize -= Long.BYTES;
                this.keyBlockIndex = frame.readLong();
                break;
            case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
                uuidLen = frame.readShort();
                frameSize -= Short.BYTES;
                id = new byte[uuidLen];
                frame.readBytes(id);
                this.uuid = new String(id);
                frameSize -= uuidLen;

                this.keyBlockIndex = frame.readLong();
                frameSize -= Long.BYTES;

                this.respOpId = frame.readShort();
                frameSize -= Short.BYTES;

                frameSize -= frame.readableBytes();
                frame.readBytes(payBuf, frame.readableBytes());
                payLoadMode = !(frameSize == 0);
                break;
            }
        } else {
            int k = frame.readableBytes();
            frameSize -= frame.readableBytes();
            frame.readBytes(payBuf, k);
            payLoadMode = !(frameSize == 0);
        }
        return !payLoadMode;
    }

    public String opIdToString(short id) {
        switch (id) {
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
            return "REQ_GET_ALLOC_KP_BLOCK";
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
            return "REQ_POST_ALLOC_KP_BLOCK";
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            return "REQ_GET_KP_BLOCK_INDEX";
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
            return "REQ_POST_PEER_ALLOC_KP_BLOCK";
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_OTP_BLOCK_INDEX:
            return "REQ_POST_OTP_BLOCK_INDEX";
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            return "REQ_POST_KP_BLOCK_INDEX";
        default:
            return "";
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("QNLRequest:%n  opId: %s%n  srcSiteId: %s%n  dstSiteId: %s%n",
                   opIdToString(opId), srcSiteId, dstSiteId);
        switch (opId) {
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_ALLOC_KP_BLOCK:
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_GET_KP_BLOCK_INDEX:
            break;
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_PEER_ALLOC_KP_BLOCK:
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_ALLOC_KP_BLOCK:
        case com.uwaterloo.qkd.qnl.utils.QNLConstants.REQ_POST_KP_BLOCK_INDEX:
            fmt.format("  KeyBlockIndex: %s%n", keyBlockIndex);
            fmt.format("  UUID: %s%n", uuid);
            break;
        }
        fmt.close();
        return sb.toString();
    }
}

