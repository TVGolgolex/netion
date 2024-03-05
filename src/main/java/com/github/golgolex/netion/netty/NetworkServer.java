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
import com.github.golgolex.netion.netty.protocol.auth.AuthPacketInHandler;
import com.github.golgolex.netion.netty.protocol.packet.PacketManager;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.*;
import io.netty5.channel.epoll.Epoll;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import io.netty5.util.concurrent.Future;
import lombok.Getter;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Getter
public class NetworkServer extends ChannelInitializer<Channel> {

    public static NetworkServer NETWORK_SERVER;

    private final EventLoopGroup workerGroup = new MultithreadEventLoopGroup(NetworkingUtils.factory());

    private final EventLoopGroup bossGroup = new MultithreadEventLoopGroup(NetworkingUtils.factory());

    private final PacketManager packetManager = new PacketManager();

    private final List<NetworkClientAuth> clientAuths = new ArrayList<>();

    private final List<NetworkClient> clients = new ArrayList<>();

    private final List<UUID> authUniqueIdToIgnore;

    private final ConnectableAddress connectableAddress;

    private ServerBootstrap serverBootstrap;

    private SslContext sslContext;

    private boolean connected = false;

    public NetworkServer(ConnectableAddress connectableAddress,
                         List<UUID> authUniqueIdToIgnore)
    {
        this.authUniqueIdToIgnore = authUniqueIdToIgnore;
        this.connectableAddress = connectableAddress;
        NETWORK_SERVER = this;
    }

    public void tryBind(boolean ssl) {
        if (ssl) {
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
            try {
                sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }

        serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .option(ChannelOption.AUTO_READ, true)
                .channelFactory(NetworkingUtils.serverChannelFactory())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(this);

        this.packetManager.registerHandler(-400, AuthPacketInHandler.class);
        Netion.log("Using " + (Epoll.isAvailable() ? "Epoll native transport" : "NIO transport"));
        Netion.log("Try to bind to " + connectableAddress.getHostName() + ':' + connectableAddress.getPort() + "...");

        Future<Channel> channelFuture = serverBootstrap.bind(connectableAddress.getHostName(), connectableAddress.getPort()).addListener(future -> {
            if (future.isSuccess()) {
                Runtime.getRuntime().addShutdownHook(new Thread(this::close));
                Netion.log(Netion.BRANDING + " is listening @" + connectableAddress.getHostName() + ':' + connectableAddress.getPort());
                connected = true;
            } else {
                Netion.log("Failed to bind @" + connectableAddress.getHostName() + ':' + connectableAddress.getPort());
            }
        });

        new Thread(() -> {
            try {
                channelFuture.asStage().get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void close() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Netion.log("Channel [" + channel.remoteAddress().toString() + "] connecting...");

        if (sslContext != null) {
            channel.pipeline().addLast(sslContext.newHandler(channel.bufferAllocator()));
        }

        NetworkClientAuth networkClientAuth = new NetworkClientAuth(
                channel,
                this
        );
        this.clientAuths.add(networkClientAuth);
        NetworkingUtils.initChannel(channel).pipeline().addLast(
                "client",
                networkClientAuth
        );
    }

}
