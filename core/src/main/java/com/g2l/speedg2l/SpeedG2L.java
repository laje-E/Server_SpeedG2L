package com.g2l.speedg2l;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.g2l.speedg2l.entidades.Entidad;
import com.g2l.speedg2l.pantallas.PantallaMenu;
import com.g2l.speedg2l.utilidades.Render;

import java.util.ArrayList;

public class SpeedG2L extends Game {

    private SpriteBatch b;

    @Override
    public void create() {
        this.setScreen(new PantallaMenu(this));
        b = Render.batch;
    }

    @Override
    public void render() {
        Render.limpiarPantalla();
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();

        b.dispose();
    }
}
