package com.github.golgolex.netion.netty.protocol.codec;

/*
 * Copyright 2024 netion contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.golgolex.netion.Netion;
import com.github.golgolex.netion.netty.NetworkClient;
import com.github.golgolex.netion.netty.NetworkServer;
import com.github.golgolex.netion.netty.protocol.IProtocol;
import com.github.golgolex.netion.netty.protocol.ProtocolProvider;
import com.github.golgolex.netion.netty.protocol.ProtocolStream;
import com.github.golgolex.netion.netty.protocol.buf.ProtocolBuffer;
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import com.github.golgolex.netion.utilitity.StringUtil;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;

public class ProtocolInDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {

        if (!ctx.channel().isActive() || in.readableBytes() <= 0) {
            return;
        }

        String logId = StringUtil.generateRandomString(9);
        StringBuilder auth = new StringBuilder(logId + ";" + ctx.channel().remoteAddress() + ";auth=");

        if (NetworkServer.NETWORK_SERVER != null) {
            for (NetworkClient client : NetworkServer.NETWORK_SERVER.getClients()) {
                if (client.getChannel().equals(ctx.channel())) {
                    auth.append(client.getAuthentication().namespace()).append(":").append(client.getAuthentication().uniqueId());
                    break;
                } else {
                    auth.append("x");
                }
            }
        }

        if (in.readableBytes() < 4) {
            Netion.debug("[" + auth + "]" + "Decode not enough readableBytes(" + in.readableBytes() + ";channel=" + ctx.channel().remoteAddress().toString() + ")");
            return;
        }

        int protocolId = in.readInt();

        Netion.debug("Buffer capacity: " + in.capacity() + ", position: ");

        ProtocolBuffer protocolBuffer = ProtocolProvider.protocolBuffer(in);
        IProtocol iProtocol = ProtocolProvider.getProtocol(protocolId);

        if (iProtocol == null) {
            Netion.debug("[" + auth + "]" + "No protocol with id (" + protocolId + ") registered");
            return;
        }

        Netion.debug("[" + auth + "]" + "Decode iProtocol (" + protocolId + ")");

        ProtocolStream protocolStream = iProtocol.createEmptyElement();
        Netion.debug("[" + auth + "]" + "Decode protocolStream.read(" + protocolStream.getClass().getSimpleName() + ")");
        protocolStream.read(protocolBuffer.clone());
        if (protocolStream instanceof Packet packet) {
            Netion.debug("[" + auth + "]" + "Decode protocolStream.read(" + packet.getId() + ":" + packet.getUniqueId() + ":" + protocolStream.getClass().getSimpleName() + ")");
        }
        Netion.debug("[" + auth + "]" + "Decode protocolStream.fireChannelRead(" + protocolStream.getClass().getSimpleName() + ")");
        ctx.fireChannelRead(protocolStream);

        in.resetOffsets();
    }
}
