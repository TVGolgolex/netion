/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package com.github.golgolex.netion.netty.protocol.file;


import com.github.golgolex.netion.netty.protocol.IProtocol;
import com.github.golgolex.netion.netty.protocol.ProtocolStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by Tareko on 09.09.2017.
 */
public class FileProtocol implements IProtocol {

    @Override
    public int getId() {
        return 2;
    }

    @Override
    public Collection<Class<?>> getAvailableClasses() {
        return Arrays.asList(File.class, Path.class, FileDeploy.class);
    }

    @Override
    public ProtocolStream createElement(Object element) {
        if (element.getClass().equals(File.class)) {
            try {
                byte[] input = Files.readAllBytes(Paths.get(((File) element).getPath()));
                String dest = ((File) element).getPath();
                return new FileDeploy(dest, input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (element instanceof FileDeploy) {
            return (FileDeploy) element;
        }

        return null;
    }

    @Override
    public ProtocolStream createEmptyElement() {
        return new FileDeploy();
    }
}
