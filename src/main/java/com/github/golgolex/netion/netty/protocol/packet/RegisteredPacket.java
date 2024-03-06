package com.github.golgolex.netion.netty.protocol.packet;

import lombok.Getter;

@Getter
public class RegisteredPacket {

    private final Class<? extends Packet> packetClass;

    public RegisteredPacket(Class<? extends Packet> packetClass)
    {
        this.packetClass = packetClass;
    }
}
