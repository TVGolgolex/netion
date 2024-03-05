package com.github.golgolex.netion.netty.protocol.auth;

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
import com.github.golgolex.netion.netty.NetworkClientAuth;
import com.github.golgolex.netion.netty.NetworkServer;
import com.github.golgolex.netion.netty.document.Document;
import com.github.golgolex.netion.netty.protocol.packet.PacketInHandler;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;

import java.util.UUID;

public class AuthPacketInHandler extends PacketInHandler {
    @Override
    public void handleInput(Document data, PacketSender packetSender, ChannelHandlerContext ctx)
    {
        Auth auth = new Auth(data.getString("namespace"), UUID.fromString(data.getString("uniqueId")));
        NetworkClientAuth client = (NetworkClientAuth) packetSender;

        Channel channel = client.getChannel();
        channel.pipeline().remove("client");

        if (NetworkServer.NETWORK_SERVER.getAuthUniqueIdToIgnore().contains(auth.uniqueId())) {
            Netion.log("Authentication request by " + auth.namespace() + "#" + auth.uniqueId() + " but it's marked as IGNORE");
            return;
        }

        NetworkServer.NETWORK_SERVER.getClientAuths().removeIf(networkClientAuth -> networkClientAuth.equals(client));
        NetworkClient networkClient = new NetworkClient(auth, channel);
        channel.pipeline().addLast(networkClient);
        NetworkServer.NETWORK_SERVER.getClients().add(networkClient);
    }
}
