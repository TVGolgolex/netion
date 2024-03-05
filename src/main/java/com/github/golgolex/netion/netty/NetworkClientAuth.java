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
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import com.github.golgolex.netion.utilitity.scheduler.TaskScheduler;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.Getter;

@Getter
public class NetworkClientAuth extends SimpleChannelInboundHandler<Packet> implements PacketSender {

    private final long connected;
    private final Channel channel;
    private final NetworkServer networkServer;

    public NetworkClientAuth(Channel channel, NetworkServer networkServer) {
        this.channel = channel;
        this.connected = System.currentTimeMillis();
        this.networkServer = networkServer;
    }

    @Override
    public void send(Object object) {
        channel.writeAndFlush(object);
        Netion.debug("send (object) " + object.getClass().getSimpleName() + "]");
    }

    @Override
    public void sendSynchronized(Object object) {
        channel.writeAndFlush(object);
        Netion.debug("sendSynchronized (object) " + object.getClass().getSimpleName() + "]");
    }

    @Override
    public void sendASynchronized(Object object) {
        TaskScheduler.runtimeScheduler().schedule(() -> {
            channel.writeAndFlush(object);
            Netion.debug("sendASynchronized (object) " + object.getClass().getSimpleName() + "]");
        });
    }

    @Override
    public void send(IProtocol protocol, Object element) {
        send(new ProtocolRequest(protocol.getId(), element));
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
    public void sendASynchronized(IProtocol protocol, Object element) {
        sendASynchronized(new ProtocolRequest(protocol.getId(), element));
    }

    @Override
    public void sendSynchronized(int id, Object element) {
        sendSynchronized(new ProtocolRequest(id, element));
    }

    @Override
    public void sendSynchronized(IProtocol protocol, Object element) {
        sendSynchronized(new ProtocolRequest(protocol.getId(), element));
    }

    @Override
    public void sendPacket(Packet... packets) {
        for (Packet packet : packets) {
            channel.writeAndFlush(packet);
            Netion.debug("sendPacket (packets) " + packet.getClass().getSimpleName() + "[id=" + packet.getId() + ";" + "uuid=" + packet.getUniqueId() + "]");
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        channel.writeAndFlush(packet);
        Netion.debug("sendPacket " + packet.getClass().getSimpleName() + "[id=" + packet.getId() + ";" + "uuid=" + packet.getUniqueId() + "]");
    }

    @Override
    public void sendPacketSynchronized(Packet packet) {
        channel.writeAndFlush(packet);
        Netion.debug("sendPacketSynchronized " + packet.getClass().getSimpleName() + "[id=" + packet.getId() + ";" + "uuid=" + packet.getUniqueId() + "]");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!channel.isActive() || !channel.isOpen() || !channel.isWritable())) {
            channel.close();
        }
        Netion.log("Channel disconnected [" + channel.remoteAddress().toString() + "]");
        this.networkServer.getClientAuths().remove(this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Netion.debug("ChannelRead: " + msg.getClass().getSimpleName());
        if (msg instanceof Packet packet) {
            Netion.debug("Received Packet [id=" + packet.getId() + ";class=" + packet.getClass().getSimpleName() + "] on " + channel.remoteAddress().toString());
            networkServer.getPacketManager().dispatchPacket(packet, this, ctx);
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Packet msg) throws Exception {
        //does nothing
        Netion.debug("messageReceived");
        Netion.debug("Received Packet [id=" + msg.getId() + ";class=" + msg.getClass().getSimpleName() + "] on " + channel.remoteAddress().toString());
        networkServer.getPacketManager().dispatchPacket(msg, this, ctx);
    }
}
