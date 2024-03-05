/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package com.github.golgolex.netion.netty.protocol.packet;


import com.github.golgolex.netion.netty.protocol.IProtocol;
import com.github.golgolex.netion.netty.protocol.ProtocolStream;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by Tareko on 09.09.2017.
 */
public class PacketProtocol implements IProtocol {

    @Override
    public int getId() {
        return 1;
    }

    @Override
    public Collection<Class<?>> getAvailableClasses() {
        return Collections.singletonList(Packet.class);
    }

    @Override
    public ProtocolStream createElement(Object element) {
        if (element instanceof Packet) {
            return (Packet) element;
        }
        return null;
    }

    @Override
    public ProtocolStream createEmptyElement() {
        return new Packet();
    }
}
