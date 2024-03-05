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
import com.github.golgolex.netion.netty.document.Document;
import com.github.golgolex.netion.netty.protocol.ProtocolStream;
import com.github.golgolex.netion.netty.protocol.buf.ProtocolBuffer;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Packet extends ProtocolStream {

    protected int id;
    protected Document data;
    protected UUID uniqueId = null;

    public Packet() {
    }

    public Packet(int id) {
        this.id = id;
        this.data = new Document();
    }

    public Packet(Document data) {
        this.data = data;
        this.id = 0;
    }

    public Packet(int id, Document data) {
        this.id = id;
        this.data = data;
    }

    public Packet(UUID uniqueId, int id, Document data) {
        this.uniqueId = uniqueId;
        this.id = id;
        this.data = data;
    }

    @Override
    public void read(ProtocolBuffer in) {
        this.id = in.readInt();
        UUID readUUID = in.readUUID();

        if (readUUID != Netion.SYSTEM_UUID) {
            this.uniqueId = readUUID;
        } else {
            this.uniqueId = null;
        }

        Document readDocument = in.readDocument();
        if (!readDocument.contains("packet_state_x1_empty")) {
            this.data = readDocument;
        } else {
            this.data = new Document();
        }
    }

    @Override
    public void write(ProtocolBuffer outPut) {
        outPut.writeInt(this.id);

        if (this.uniqueId == null) {
            this.uniqueId = Netion.SYSTEM_UUID;
        }
        outPut.writeUUID(this.uniqueId);

        if (this.data.isEmpty()) {
            this.data = new Document("packet_state_x1_empty", "empty");
        }
        outPut.writeDocument(this.data);
    }
}
