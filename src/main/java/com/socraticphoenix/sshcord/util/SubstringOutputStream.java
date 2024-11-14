package com.socraticphoenix.sshcord.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class SubstringOutputStream extends ByteArrayOutputStream {

    public SubstringOutputStream() {
    }

    public SubstringOutputStream(int size) {
        super(size);
    }

    public String toString(int start, int end, Charset charset) {
        return new String(this.buf, start, end - start, charset);
    }

}
