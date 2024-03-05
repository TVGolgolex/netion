/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package com.github.golgolex.netion.netty.protocol;

import java.util.Collection;

/**
 * Created by Tareko on 09.09.2017.
 */
public interface IProtocol {

    int getId();

    Collection<Class<?>> getAvailableClasses();

    ProtocolStream createElement(Object element) throws Exception;

    ProtocolStream createEmptyElement();

}