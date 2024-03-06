/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package com.github.golgolex.netion.netty.protocol.file;

import com.github.golgolex.netion.netty.protocol.ProtocolStream;
import com.github.golgolex.netion.netty.protocol.buf.ByteBuffer;
import com.github.golgolex.netion.netty.protocol.buf.ProtocolBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * Created by Tareko on 09.09.2017.
 */
public class FileDeploy extends ProtocolStream {

    protected String dest;

    protected byte[] bytes;

    public FileDeploy(String dest, byte[] bytes) {
        this.dest = dest;
        this.bytes = bytes;
    }

    public FileDeploy() {
    }

    @Override
    public void write(ByteBuffer out) {
        out.writeString(dest);
        out.writeStringCollection(Stream.of(bytes).map(String::valueOf).toList());
    }

    @Override
    public void read(ByteBuffer in) {
        if (in.getBuffer().readableBytes() != 0) {
            this.dest = in.readString();
            this.bytes = convertStringCollectionToByteArray(in.readStringCollection());
            toWrite();
        }
    }

    private byte[] convertStringCollectionToByteArray(Collection<String> stringCollection) {
        StringJoiner stringJoiner = new StringJoiner(",");

        for (String str : stringCollection) {
            stringJoiner.add(str);
        }

        return stringJoiner.toString().getBytes();
    }

    public void toWrite() {
        try {
            File file = new File(dest);
            file.getParentFile().mkdirs();
            file.createNewFile();

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
