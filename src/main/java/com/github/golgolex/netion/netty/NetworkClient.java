package com.github.golgolex.netion.netty;

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
import com.github.golgolex.netion.netty.protocol.ProtocolRequest;
import com.github.golgolex.netion.netty.protocol.auth.Auth;
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import com.github.golgolex.netion.utilitity.scheduler.TaskScheduler;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.Getter;

import java.io.IOException;

@Getter
public class NetworkClient extends SimpleChannelInboundHandler implements PacketSender {

    private final Auth authentication;

    private Channel channel;

    public NetworkClient(Auth authentication, Channel channel) {
        this.authentication = authentication;
        this.channel = channel;
        Netion.log("Channel connected [" + channel.remoteAddress()
                .toString() + "/authId=" + authentication.namespace() + "#" + authentication.uniqueId() + ']');
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!channel.isActive() || !channel.isOpen() || !channel.isWritable())) {
            Netion.log("Channel disconnected [" + channel.remoteAddress()
                    .toString() + "/authId=" + authentication.namespace() + "#" + authentication.uniqueId() + ']');
            ctx.close();
            channel = null;
            NetworkServer.NETWORK_SERVER.getAuthenticatedClients().removeIf(networkClient -> networkClient.authentication.equals(authentication));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
        //TODO:
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        Netion.debug("messageReceived");

        if (!(msg instanceof Packet packet)) {
            Netion.debug("msg is not a instance of Packet packet [" + msg.getClass().getSimpleName() + "]");
            return;
        }

        Netion.debug("Receiving Packet (id=" + packet.getId() + ";dataLength=" + packet
                .getData()
                .size() + ") by " + authentication.namespace() + "#" + authentication.uniqueId());
        NetworkServer.NETWORK_SERVER.getPacketManager().dispatchPacket(packet, this, ctx);
    }

    @Override
    public void send(Object object) {
        Netion.debug("Sending Object [" + object.getClass().getSimpleName() + "] to " + authentication.namespace() + "#" + authentication.uniqueId().toString());
        channel.writeAndFlush(object);
    }

    @Override
    public void sendSynchronized(Object object) {
        this.send(object);
    }

    @Override
    public void sendASynchronized(Object object) {
        TaskScheduler.runtimeScheduler().schedule(new Runnable() {
            @Override
            public void run() {
                Netion.debug("Sending Object [" + object.getClass().getSimpleName() + "] to " + authentication.namespace() + "#" + authentication.uniqueId().toString());
                channel.writeAndFlush(object);
            }
        });
    }

    @Override
    public void send(IProtocol iProtocol, Object element) {
        send(new ProtocolRequest(iProtocol.getId(), element));
    }

    @Override
    public void send(int id, Object element) {
        send(new ProtocolRequest(id, element));
    }

    @Override
    public void sendASynchronized(int id, Object element) {
        sendASynchronized(new ProtocolRequest(id, element));
    }

    @Override
    public void sendASynchronized(IProtocol iProtocol, Object element) {
        sendASynchronized(new ProtocolRequest(iProtocol.getId(), element));
    }

    @Override
    public void sendSynchronized(int id, Object element) {
        sendSynchronized(new ProtocolRequest(id, element));
    }

    @Override
    public void sendSynchronized(IProtocol iProtocol, Object element) {
        sendSynchronized(new ProtocolRequest(iProtocol.getId(), element));
    }

    @Override
    public void sendPacket(Packet... packets) {
        for (Packet packet : packets) {
            sendPacket(packet);
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        Netion.debug("Sending Packet " + packet.getClass().getSimpleName() + " (id=" + packet
                .getId() + ";dataLength=" + packet
                .getData()
                .size() + ") to " + authentication.namespace() + "#" + authentication.uniqueId().toString());

        if (channel == null) {
            Netion.debug("No channel to send packet");
            return;
        }
        channel.writeAndFlush(packet);
    }

    @Override
    public void sendPacketSynchronized(Packet packet) {
        this.sendPacket(packet);
    }
}
