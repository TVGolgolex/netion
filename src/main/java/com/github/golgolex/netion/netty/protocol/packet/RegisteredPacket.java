package com.github.golgolex.netion.netty.protocol.packet;

public class RegisteredPacket {

    private final Class<? extends Packet> packetClass;

    public RegisteredPacket(Class<? extends Packet> packetClass)
    {
        this.packetClass = packetClass;
    }
}
