/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package com.github.golgolex.netion.netty.protocol;

import com.github.golgolex.netion.netty.protocol.buf.ProtocolBuffer;
import com.github.golgolex.netion.netty.protocol.file.FileProtocol;
import com.github.golgolex.netion.netty.protocol.packet.PacketProtocol;
import io.netty5.buffer.Buffer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Tareko on 09.09.2017.
 */
public final class ProtocolProvider {

    private static final Map<Integer, IProtocol> protocols = new ConcurrentHashMap<>(0);

    static
    {
        registerProtocol(new PacketProtocol());
        registerProtocol(new FileProtocol());
    }

    private ProtocolProvider()
    {
    }

    public static ProtocolBuffer protocolBuffer(Buffer buffer)
    {
        return new ProtocolBuffer(buffer);
    }

    public static void registerProtocol(IProtocol iProtocol)
    {
        protocols.put(iProtocol.getId(), iProtocol);
    }

    public static IProtocol getProtocol(int id)
    {
        return protocols.get(id);
    }

    public static Collection<IProtocol> protocols()
    {
        return protocols.values();
    }
}
