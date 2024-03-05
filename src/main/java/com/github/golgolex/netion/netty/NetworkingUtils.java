package com.github.golgolex.netion.netty;

import com.github.golgolex.netion.netty.protocol.codec.ProtocolInDecoder;
import com.github.golgolex.netion.netty.protocol.codec.ProtocolOutEncoder;
import io.netty5.channel.*;
import io.netty5.channel.epoll.Epoll;
import io.netty5.channel.epoll.EpollHandler;
import io.netty5.channel.epoll.EpollServerSocketChannel;
import io.netty5.channel.epoll.EpollSocketChannel;
import io.netty5.channel.nio.NioHandler;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.channel.socket.nio.NioSocketChannel;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import lombok.experimental.UtilityClass;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

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

@UtilityClass
public class NetworkingUtils {

    public Channel initChannel(Channel channel) {
        channel.pipeline().addLast(
                new ProtocolInDecoder(),
                new ProtocolOutEncoder(0)
        );
        return channel;
    }

    public SslContext createSslContext() throws CertificateException, SSLException
    {
        var ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    public IoHandlerFactory factory() {
        return Epoll.isAvailable() ? EpollHandler.newFactory() : NioHandler.newFactory();
    }

    public ServerChannelFactory<? extends ServerChannel> serverChannelFactory() {
        return Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
    }

    public ChannelFactory<? extends Channel> channelFactory() {
        return Epoll.isAvailable() ? EpollSocketChannel::new : NioSocketChannel::new;
    }

}
