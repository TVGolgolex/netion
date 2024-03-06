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
import com.github.golgolex.netion.netty.protocol.file.FileDeploy;
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import com.github.golgolex.netion.netty.protocol.packet.PacketManager;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;

import java.io.IOException;

public class NetDispatcher extends SimpleChannelInboundHandler {

    private final PacketManager packetManager;
    private final PacketSender packetSender;
    private final boolean shutdownOnInactive;

    public NetDispatcher(PacketManager packetManager,
                         PacketSender packetSender,
                         boolean shutdownOnInactive)
    {
        this.packetManager = packetManager;
        this.packetSender = packetSender;
        this.shutdownOnInactive = shutdownOnInactive;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            ctx.channel().close();
            if (shutdownOnInactive) {
                System.exit(0);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        Netion.debug("ChannelRead: " + msg.getClass().getSimpleName());
        if (msg instanceof Packet packet)
        {
            Netion.debug("Received Packet [id=" + packet.getId() + ";class=" + packet.getClass().getSimpleName() + "]");
            packetManager.dispatchPacket(packet, packetSender, ctx);
        } else
        {
            if (msg instanceof FileDeploy deploy)
            {
                deploy.toWrite();
            }
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        //does nothing
        Netion.debug("messageReceived in Class (" + this.getClass().getSimpleName() + ")");
        if (msg instanceof Packet packet)
        {
            Netion.debug("Received Packet [id=" + packet.getId() + ";class=" + packet.getClass().getSimpleName() + "]");
            packetManager.dispatchPacket(packet, packetSender, ctx);
        } else
        {
            if (msg instanceof FileDeploy deploy)
            {
                deploy.toWrite();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        ctx.flush();
        Netion.debug("channelReadComplete flush");
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (!(cause instanceof IOException))
        {
            cause.printStackTrace();
        }
    }
}