package com.github.golgolex.netion.netty;

import com.github.golgolex.netion.Netion;
import com.github.golgolex.netion.netty.protocol.IProtocol;
import com.github.golgolex.netion.netty.protocol.ProtocolProvider;
import com.github.golgolex.netion.netty.protocol.ProtocolRequest;
import com.github.golgolex.netion.netty.protocol.auth.Auth;
import com.github.golgolex.netion.netty.protocol.auth.AuthPacket;
import com.github.golgolex.netion.netty.protocol.codec.NetDispatcher;
import com.github.golgolex.netion.netty.protocol.packet.Packet;
import com.github.golgolex.netion.netty.protocol.packet.PacketManager;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import com.github.golgolex.netion.utilitity.scheduler.TaskScheduler;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.*;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

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
@Getter
public final class NetworkConnection implements PacketSender {

    private final PacketManager packetManager = new PacketManager();

    private final EventLoopGroup eventLoopGroup = new MultithreadEventLoopGroup(NetworkingUtils.factory());

    private final ConnectableAddress connectableAddress;

    private Channel channel;

    private long connectionTrys = 0;

    private Runnable task;

    private SslContext sslContext;

    private Auth authentication;

    @Setter
    private boolean disableOutput = false;

    public NetworkConnection(ConnectableAddress connectableAddress)
    {
        this.connectableAddress = connectableAddress;
    }

    public NetworkConnection(ConnectableAddress connectableAddress, Runnable task)
    {
        this.connectableAddress = connectableAddress;
        this.task = task;
    }

    protected void setChannel(Channel channel)
    {
        this.channel = channel;
    }

    public String getName()
    {
        return "Network-Connector";
    }

    public boolean tryConnect(boolean ssl, Auth auth)
    {
        return tryConnect(ssl, auth, null, null);
    }

    public boolean tryConnect(boolean ssl, Auth auth, Runnable connectedTask, Runnable cancelTask) {
        this.authentication = auth;
        try {
            Netion.log("Trying to connect to " + connectableAddress.getHostName() + ":" + connectableAddress.getPort());
            if (ssl) {
                sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            }

            Bootstrap bootstrap = new Bootstrap()
                    .option(ChannelOption.AUTO_READ, true)
                    .group(eventLoopGroup)
                    .handler(new ChannelInitializer<>() {

                        @Override
                        protected void initChannel(Channel channel) throws Exception
                        {
                            if (sslContext != null) {
                                channel.pipeline().addLast(sslContext.newHandler(channel.bufferAllocator(),
                                        connectableAddress.getHostName(),
                                        connectableAddress.getPort()));
                            }

                            NetworkingUtils.initChannel(channel).pipeline().addLast(new NetDispatcher(
                                    packetManager,
                                    NetworkConnection.this,
                                    true
                            ));
                        }
                    })
                    .channelFactory(NetworkingUtils.channelFactory());
                    Runtime.getRuntime().addShutdownHook(new Thread(this::tryDisconnect));

            NetworkConnectionThread networkConnectionThread = new NetworkConnectionThread(bootstrap, this, connectedTask);

            Thread thread = new Thread(networkConnectionThread);
            thread.start();
            thread.join();
            this.channel = networkConnectionThread.getChannel();

            if (this.channel == null) {
                throw new NullPointerException("No channel provided");
            }

            if (connectedTask != null) {
                connectedTask.run();
            }

            channel.writeAndFlush(new AuthPacket(authentication));
            Netion.log("Channel connected [" + channel.remoteAddress()
                    .toString() + "/authId=" + authentication.namespace() + ";" + authentication.uniqueId().toString() + ']');

            return true;
        } catch (Exception ex)
        {
            connectionTrys++;
            Netion.log("Failed to connect... [" + connectionTrys + ']');
            Netion.log("Error: " + ex.getMessage());

            if (this.channel != null)
            {
                this.channel.close();
                this.channel = null;
            }

            if (cancelTask != null)
            {
                cancelTask.run();
            }
            return false;
        }
    }

    public boolean tryDisconnect()
    {
        if (channel != null)
        {
            channel.close();
        }
        eventLoopGroup.shutdownGracefully();
        return false;
    }

    @Override
    public void send(Object object)
    {
        if (channel == null)
        {
            return;
        }
        if (disableOutput) {
            return;
        }
        Netion.debug("Sending Object [" + object.getClass().getSimpleName() + "]");
        channel.writeAndFlush(object);
    }

    @Override
    public void sendSynchronized(Object object)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        Netion.debug("Sending Object [" + object.getClass().getSimpleName() + "]");
        channel.writeAndFlush(object);
    }

    @Override
    public void sendASynchronized(Object object)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        TaskScheduler.runtimeScheduler().schedule(() -> {
            Netion.debug("Sending Object [" + object.getClass().getSimpleName() + "]");
            channel.writeAndFlush(object);
        });
    }

    @Override
    public void send(IProtocol iProtocol, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        send(new ProtocolRequest(iProtocol.getId(), element));
    }

    @Override
    public void send(int id, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        send(new ProtocolRequest(id, element));
    }

    @Override
    public void sendASynchronized(int id, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        sendASynchronized(new ProtocolRequest(id, element));
    }

    @Override
    public void sendASynchronized(IProtocol iProtocol, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        sendASynchronized(new ProtocolRequest(iProtocol.getId(), element));
    }

    @Override
    public void sendSynchronized(int id, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        sendSynchronized(new ProtocolRequest(id, element));
    }

    @Override
    public void sendSynchronized(IProtocol iProtocol, Object element)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        sendSynchronized(new ProtocolRequest(iProtocol.getId(), element));
    }

    public void sendFile(File file)
    {
        if (channel == null) {
            return;
        }
        if (disableOutput) {
            return;
        }
        send(ProtocolProvider.getProtocol(2), file);
    }

    @Override
    public void sendPacket(Packet... packets)
    {
        if (channel == null)
        {
            return;
        }
        if (disableOutput) {
            return;
        }
        for (Packet packet : packets)
        {
            this.sendPacket(packet);
        }
    }

    @Override
    public void sendPacket(Packet packet)
    {
        if (channel == null)
        {
            return;
        }
        if (disableOutput) {
            return;
        }
        Netion.debug("Sending Packet " + packet.getClass().getSimpleName() + " (id=" + packet
                .getId() + ";dataLength=" + packet
                .getData()
                .size() + ")");
        channel.writeAndFlush(packet);
    }

    @Override
    public void sendPacketSynchronized(Packet packet)
    {
        if (channel == null)
        {
            return;
        }
        if (disableOutput) {
            return;
        }
        Netion.debug("Sending Packet " + packet.getClass().getSimpleName() + " (id=" + packet
                .getId() + ";dataLength=" + packet
                .getData()
                .size() + ")");
        channel.writeAndFlush(packet);
    }

    public boolean isConnected()
    {
        return channel != null;
    }
}
