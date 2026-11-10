package com.g2l.speedg2l.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.g2l.speedg2l.componentes.Texto;
import com.g2l.speedg2l.utilidades.Recursos;

public class PantallaInstrucciones extends Pantalla{
    private Texto texto;

    protected PantallaInstrucciones(Game juego, SpriteBatch b) {
        super(juego, b);
    }

    @Override
    public void show() {
        texto = new Texto(Recursos.FUENTE_MENU, 20, Color.WHITE, batch);
        texto.setTexto("Instrucciones");
        texto.centrar();
    }

    @Override
    public void render(float delta) {
        batch.begin();
        texto.dibujar();
        batch.end();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        texto.cerrar();
    }
}
