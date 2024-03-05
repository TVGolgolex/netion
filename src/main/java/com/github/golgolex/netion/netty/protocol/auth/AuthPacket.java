package com.github.golgolex.netion.netty.protocol.auth;

import com.github.golgolex.netion.netty.document.Document;
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import lombok.Getter;

/**
 * Created by Pascal K. on 08.02.2024.
 */

@Getter
public class AuthPacket extends Packet {

    public AuthPacket(Auth auth)
    {
        super(-400, new Document("namespace", auth.namespace())
                .append("uniqueId", auth.uniqueId().toString()));
    }
}
