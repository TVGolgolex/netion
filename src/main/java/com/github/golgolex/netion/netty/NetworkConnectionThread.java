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
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.Channel;
import lombok.Getter;

import java.util.concurrent.ExecutionException;

@Getter
public class NetworkConnectionThread implements Runnable {

    private Channel channel = null;

    private final Bootstrap bootstrap;

    private final NetworkConnection networkConnection;

    private final Runnable connectedTask;

    public NetworkConnectionThread(Bootstrap bootstrap, NetworkConnection networkConnection, Runnable connectedTask) {
        this.bootstrap = bootstrap;
        this.networkConnection = networkConnection;
        this.connectedTask = connectedTask;
    }

    @Override
    public void run() {
        try {
            this.channel = bootstrap.connect(networkConnection.getConnectableAddress().getHostName(),
                            networkConnection.getConnectableAddress().getPort()).asStage().get();
        } catch (InterruptedException | ExecutionException exception) {
            Netion.debug(exception.getMessage());
        }
    }
}
