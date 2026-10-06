package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.entidades.Jugador;

public class Cliente {

    private DireccionRed direccionRed;
    private Jugador jugador;

    public Cliente(DireccionRed direccionRed, Jugador jugador) {
        this.direccionRed = direccionRed;
        this.jugador = jugador;
    }

    public DireccionRed getDireccionRed() {
        return direccionRed;
    }

    public Jugador getJugador() {
        return jugador;
    }
}
