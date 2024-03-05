package com.github.golgolex.netion.utilitity.scheduler;

/**
 * Created by Tareko on 24.05.2017.
 */
public interface Callback<C> {

    void call(C value);

}
