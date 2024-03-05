package com.github.golgolex.netion.utilitity;

/**
 * Created by Tareko on 19.07.2017.
 */
public interface Catcher<E, V> {

    E doCatch(V key);

}
