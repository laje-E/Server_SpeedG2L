package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.componentes.Texto;

public class PosicionFinal {

    private float tiempo;
    private Texto cronometro;
    private int numeroPosicion;

    public PosicionFinal(float tiempo, Texto cronometro, int numeroPosicion){
        this.tiempo = tiempo;
        this.cronometro = cronometro;
        this.numeroPosicion = numeroPosicion;
    }

}
