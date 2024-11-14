package com.socraticphoenix.sshcord;

public class Config {
    private String token;
    private int sshTimeout;
    private int seshTimeout;
    private int bufferLimit;
    private int bufferSize;
    private int msgLimit;
    private int outputWait;
    private int deleteWait;

    public Config(String token, int sshTimeout, int seshTimeout, int bufferLimit, int bufferSize, int msgLimit, int outputWait, int deleteWait) {
        this.token = token;
        this.sshTimeout = sshTimeout;
        this.seshTimeout = seshTimeout;
        this.bufferLimit = bufferLimit;
        this.bufferSize = bufferSize;
        this.msgLimit = msgLimit;
        this.outputWait = outputWait;
    }

    public int deleteWait() {
        return this.deleteWait;
    }

    public void setDeleteWait(int deleteWait) {
        this.deleteWait = deleteWait;
    }

    public int outputWait() {
        return this.outputWait;
    }

    public void setOutputWait(int outputWait) {
        this.outputWait = outputWait;
    }

    public int msgLimit() {
        return this.msgLimit;
    }

    public void setMsgLimit(int msgLimit) {
        this.msgLimit = msgLimit;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int bufferLimit() {
        return this.bufferLimit;
    }

    public void setBufferLimit(int bufferLimit) {
        this.bufferLimit = bufferLimit;
    }

    public int seshTimeout() {
        return this.seshTimeout;
    }

    public void setSeshTimeout(int seshTimeout) {
        this.seshTimeout = seshTimeout;
    }

    public int sshTimeout() {
        return this.sshTimeout;
    }

    public void setSshTimeout(int sshTimeout) {
        this.sshTimeout = sshTimeout;
    }

    public String token() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
