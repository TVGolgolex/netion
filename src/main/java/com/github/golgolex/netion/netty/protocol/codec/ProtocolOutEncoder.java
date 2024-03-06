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
import com.github.golgolex.netion.netty.protocol.IProtocol;
import com.github.golgolex.netion.netty.protocol.ProtocolProvider;
import com.github.golgolex.netion.netty.protocol.ProtocolRequest;
import com.github.golgolex.netion.netty.protocol.ProtocolStream;
import com.github.golgolex.netion.netty.protocol.buf.ByteBuffer;
import com.github.golgolex.netion.netty.protocol.buf.ProtocolBuffer;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.MessageToByteEncoder;

public class ProtocolOutEncoder extends MessageToByteEncoder {

    private final int bufferAllocation;

    public ProtocolOutEncoder(int bufferAllocation)
    {
        this.bufferAllocation = (bufferAllocation < 0 ? 1024 : bufferAllocation);
    }

    @Override
    protected Buffer allocateBuffer(ChannelHandlerContext ctx, Object msg) throws Exception {
        return ctx.bufferAllocator().allocate(0);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, Buffer out) throws Exception {
//        ProtocolBuffer protocolBuffer = ProtocolProvider.protocolBuffer(out);
        ByteBuffer byteBuffer = new ByteBuffer(out);

        if (msg instanceof ProtocolRequest protocolRequest) {
            IProtocol iProtocol = ProtocolProvider.getProtocol(protocolRequest.getId());
            if (iProtocol == null) {
                Netion.debug("IProtocol for request " + protocolRequest.getId() + " is null");
                return;
            }
            ProtocolStream protocolStream = iProtocol.createElement(protocolRequest.getElement());
            if (protocolStream == null) {
                Netion.debug("ProtocolStream for request " + protocolRequest.getElement().getClass().getSimpleName() + " is null");
                return;
            }
            byteBuffer.writeInt(iProtocol.getId());
            protocolStream.write(byteBuffer);
            Netion.debug("Encode protocolStream.write(" + protocolStream.getClass().getSimpleName() +")");
        } else {
            for (IProtocol iProtocol : ProtocolProvider.protocols()) {
                ProtocolStream protocolStream = iProtocol.createElement(msg);
                if (protocolStream != null) {
                    byteBuffer.writeInt(iProtocol.getId());
                    protocolStream.write(byteBuffer);
                    Netion.debug("Encode [for] protocolStream.write(" + protocolStream.getClass().getSimpleName() +")");
                    break;
                }
            }
        }

//        out.writeBytes(protocolBuffer);
    }
}
