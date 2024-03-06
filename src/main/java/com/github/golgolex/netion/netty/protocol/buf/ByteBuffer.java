package com.github.golgolex.netion.netty.protocol.buf;

import com.github.golgolex.netion.netty.document.Document;
import com.github.golgolex.netion.netty.io.CallableDecoder;
import com.github.golgolex.netion.netty.io.CallableEncoder;
import com.github.golgolex.netion.netty.io.ProtocolDecoder;
import com.github.golgolex.netion.netty.io.ProtocolEncoder;
import io.netty5.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Getter
public class ByteBuffer {

    private final Buffer buffer;

    public ByteBuffer(Buffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer writeInt(int value) {
        this.buffer.writeInt(value);
        return this;
    }

    public int readInt() {
        return this.buffer.readInt();
    }

    public ByteBuffer writeString(String value) {
        var bytes = value.getBytes();
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public String readString() {
        return this.buffer.readCharSequence(this.buffer.readInt(), StandardCharsets.UTF_8).toString();
    }

    public void writeDocument(Document document) {
        this.writeString(document.convertToJsonString());
    }

    public Document readDocument() {
        return Document.load(readString());
    }

    public ByteBuffer writeBoolean(Boolean booleanValue) {
        this.buffer.writeBoolean(booleanValue);
        return this;
    }

    public boolean readBoolean() {
        return this.buffer.readBoolean();
    }

    public ByteBuffer writeUUID(UUID uuid) {
        this.buffer.writeLong(uuid.getMostSignificantBits());
        this.buffer.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.buffer.readLong(), this.buffer.readLong());
    }

    public ByteBuffer writeEnum(Enum<?> value) {
        this.buffer.writeInt(value.ordinal());
        return this;
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[this.buffer.readInt()];
    }


    public ByteBuffer writeLong(long value) {
        this.buffer.writeLong(value);
        return this;
    }

    public long readLong() {
        return this.buffer.readLong();
    }

    public ByteBuffer writeFloat(float value) {
        this.buffer.writeFloat(value);
        return this;
    }

    public float readFloat() {
        return this.buffer.readFloat();
    }

    public ByteBuffer writeDouble(double value) {
        this.buffer.writeDouble(value);
        return this;
    }

    public double readDouble() {
        return this.buffer.readDouble();
    }

    public <T extends ProtocolEncoder> void writeCollection(final Collection<T> collection)
    {
        this.writeCollection(collection, ProtocolEncoder::write);
    }

    public <T extends ProtocolDecoder> List<T> readCollection(final Supplier<T> factory)
    {
        return this.readCollection(buffer -> {
            T instance = factory.get();
            instance.read(this);
            return instance;
        });
    }

    public <T> void writeCollection(final Collection<T> collection, final CallableEncoder<T> encoder)
    {
        this.writeInt(collection.size());
        for (final T entry : collection) {
            encoder.write(entry, this);
        }
    }

    public <T> List<T> readCollection(final CallableDecoder<T> decoder)
    {
        var size = this.readInt();
        final List<T> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) data.add(decoder.read(this));
        return data;
    }

    public void writeIntCollection(final Collection<Integer> collection)
    {
        this.writeCollection(collection, (data, buffer) -> buffer.writeInt(data));
    }

    public List<Integer> readIntCollection()
    {
        return this.readCollection(ByteBuffer::readInt);
    }

    public void writeStringCollection(final Collection<String> collection)
    {
        this.writeCollection(collection, (data, buffer) -> buffer.writeString(data));
    }

    public List<String> readStringCollection()
    {
        return this.readCollection(ByteBuffer::readString);
    }

    public void writeUuidCollection(final Collection<UUID> collection)
    {
        this.writeCollection(collection, (data, buffer) -> buffer.writeUUID(data));
    }

    public List<UUID> readUuidCollection()
    {
        return this.readCollection(ByteBuffer::readUUID);
    }

}
