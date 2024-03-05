package com.github.golgolex.netion.netty.protocol.packet;

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
import com.github.golgolex.netion.netty.document.Document;
import com.github.golgolex.netion.netty.protocol.packet.result.Result;
import com.github.golgolex.netion.netty.protocol.sender.PacketSender;
import com.github.golgolex.netion.utilitity.CollectionWrapper;
import com.github.golgolex.netion.utilitity.Value;
import com.github.golgolex.netion.utilitity.scheduler.TaskScheduler;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class PacketManager {

    private final Map<Integer, Collection<Class<? extends PacketInHandler>>> packetHandlers = new ConcurrentHashMap<>(0);

    private final Map<Integer, RegisteredPacket> registeredPackets = new ConcurrentHashMap<>(0);

    private final Map<UUID, Value<Result>> synchronizedHandlers = new ConcurrentHashMap<>();

    private final Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

    private final TaskScheduler executorService = new TaskScheduler(1);

    private final List<Integer> packetIdsWhichNotResend = new ArrayList<>();

    @Setter
    private boolean resendPacketsToActiveChannels;

    public void registerNotResendPacket(Packet packet) {
        if (packet.getId() < 1) {
            Netion.log("Cannot mark Packet " + packet.getClass().getSimpleName() + " as ignored for packet resend because id is lower than 1");
            return;
        }
        if (packetIdsWhichNotResend.contains(packet.getId())) {
            Netion.log("PacketId [" + packet.getId() + "] is already marked as ignored for packet resend");
        }
        this.packetIdsWhichNotResend.add(packet.getId());
    }

    public void registerNotResendPacket(int id) {
        if (packetIdsWhichNotResend.contains(id)) {
            Netion.log("PacketId [" + id + "] is already marked as ignored for packet resend");
        }
        this.packetIdsWhichNotResend.add(id);
    }

    /*public void registerPacket(int id, Packet packet) throws PacketRegistrationException
    {
        this.registerPacket(id, packet.getClass());
    }

    public void registerPacket(int id, Class<? extends Packet> packet) throws PacketRegistrationException
    {
        if (this.containsPacket(id)) {
            throw new PacketRegistrationException("PacketID is already in use");
        }
        RegisteredPacket registeredPacket = new RegisteredPacket(packet);
        this.registeredPackets.put(id, registeredPacket);
    }

    public int packetId(Class<? extends Packet> packetClass) {
        for (var packetId : this.registeredPackets.keySet()) {
            if (this.registeredPackets.get(packetId).getClass().equals(packetClass))
            {
                return packetId;
            }
        }
        return -1;
    }

    public boolean containsPacket(int id)
    {
        return this.registeredPackets.containsKey(id);
    }

    public boolean containsPacket(Class<? extends Packet> packet)
    {
        return this.registeredPackets.values().stream().anyMatch(registeredPacket -> registeredPacket.packetClass().equals(packet));
    }*/

    public void registerHandler(int id, Class<? extends PacketInHandler> packetHandlerClass) {
        if (!this.packetHandlers.containsKey(id)) {
            this.packetHandlers.put(id, new ArrayList<>());
        }
        this.packetHandlers.get(id).add(packetHandlerClass);
    }

    public Result sendQuery(Packet packet, PacketSender packetSender) {
        Netion.debug("SendQuery " + packet.getClass().getSimpleName() + "[packetUniqueId=" + packet.uniqueId + ";dataLength=" + (packet
                .data == null ? "-1" :
                packet.data.size()) + ")");
        UUID uniqueId = UUID.randomUUID();
        packet.uniqueId = uniqueId;
        Netion.debug("SendQuery set uniqueID" + packet.getClass().getSimpleName() + "[packetUniqueId=" + packet.uniqueId + ";dataLength=" + (packet
                .data == null ? "-1" :
                packet.data.size()) + ")");
        Value<Result> handled = new Value<>(null);
        synchronizedHandlers.put(uniqueId, handled);
        executorService.schedule(() -> {
            packetSender.sendPacket(packet);
            Netion.debug("SendQuery sendPacket " + packet.getClass().getSimpleName() + "[packetUniqueId=" + packet.uniqueId + ";dataLength=" + (packet
                    .data == null ? "-1" :
                    packet.data.size()) + ")");
        });

        int i = 0;

        while (synchronizedHandlers.get(uniqueId).getValue() == null && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        if (i >= 4999) {
            synchronizedHandlers.get(uniqueId).setValue(new Result(uniqueId, new Document()));
            Netion.debug("SendQuery setValue to empty of " + uniqueId);
        }

        Value<Result> values = synchronizedHandlers.get(uniqueId);
        Netion.debug("SendQuery return value " + uniqueId + ":" + values.getValue().getResult().size());
        synchronizedHandlers.remove(uniqueId);
        return values.getValue();
    }

    public boolean dispatchPacket(Packet incoming,
                                  PacketSender packetSender,
                                  ChannelHandlerContext ctx) {
        Netion.debug("DispatchPacket " + incoming.getClass().getSimpleName() + "[packetUniqueId=" + incoming.uniqueId + ";dataLength=" + (incoming
                                                                    .data == null ? "-1" :
                                                                    incoming.data.size()) + ") by " + ctx.channel()
                                                                                                      .remoteAddress());
        if (incoming.uniqueId != null
                && synchronizedHandlers.containsKey(incoming.uniqueId))
        {
            Netion.debug("SynchronizedHandlers.containsKey " + incoming.uniqueId);
            Result result = new Result(incoming.uniqueId, incoming.data);
            Value<Result> x = synchronizedHandlers.get(incoming.uniqueId);
            Netion.debug("setValue " + incoming.uniqueId);
            x.setValue(result);
            return false;
        }

        Collection<PacketInHandler> handlers = buildHandlers(incoming.id);
        CollectionWrapper.iterator(handlers, handler -> {
            if (incoming.uniqueId != null) {
                handler.packetUniqueId = incoming.uniqueId;
            }
            handler.packetId = incoming.id;
            handler.handleInput(incoming.data, packetSender, ctx);
        });

        if (this.resendPacketsToActiveChannels
                                                && NetworkServer.NETWORK_SERVER != null
                                                && NetworkServer.NETWORK_SERVER.isConnected()) {
            if (this.packetIdsWhichNotResend.contains(incoming.id)) {
                Netion.debug("Resend Packet [" + incoming.id + "/" + incoming.uniqueId + "] is blocked for resending");
                return true;
            }
            for (NetworkClient client : NetworkServer.NETWORK_SERVER.getClients()) {
                if (packetSender instanceof NetworkClient networkClient && networkClient
                                                                            .getAuthentication()
                                                                            .equals(client.getAuthentication())) {
                    Netion.debug("Resend Packet [" + incoming.id + "/" + incoming.uniqueId + "] is blocked for sending to self client");
                    return true;
                }
                client.sendPacket(incoming);
                Netion.debug("Resend packet [" + incoming.id + "/" + incoming.uniqueId + "] to " + client
                                                                                                .getAuthentication()
                                                                                                .namespace() + "#" + client
                                                                                                    .getAuthentication()
                                                                                                    .uniqueId());
            }
        }
        return true;
    }

    public Collection<PacketInHandler> buildHandlers(int id) {
        Collection<PacketInHandler> packetIn = new LinkedList<>();
        if (packetHandlers.containsKey(id)) {
            for (Class<? extends PacketInHandler> handlers : packetHandlers.get(id)) {
                try {
                    packetIn.add(handlers.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    return null;
                }
            }
        }
        return packetIn;
    }

}
