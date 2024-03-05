package com.github.golgolex.netion.netty.protocol.packet;

import lombok.Getter;

import java.lang.reflect.Constructor;
import java.util.Arrays;

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
public class OldRegisteredPacket {

    private final Class<? extends Packet> packetClass;

    private final Constructor<? extends Packet> constructor;

    public OldRegisteredPacket(final Class<? extends Packet> packetClass) throws NoSuchMethodException
    {
        this.packetClass = packetClass;
        var emptyConstructorList = Arrays.stream(packetClass.getConstructors()).filter(constructor -> constructor.getParameterCount() == 0).toList();
        if (emptyConstructorList.isEmpty())
        {
            throw new NoSuchMethodException("Packet is missing no-args-constructor");
        }
        this.constructor = (Constructor<? extends Packet>) emptyConstructorList.get(0);
    }
}
