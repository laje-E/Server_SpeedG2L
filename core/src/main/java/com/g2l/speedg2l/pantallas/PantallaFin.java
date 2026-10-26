
package com.g2l.speedg2l.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.g2l.speedg2l.componentes.Boton;
import com.g2l.speedg2l.componentes.PanelBotones;
import com.g2l.speedg2l.componentes.Texto;
import com.g2l.speedg2l.servidor.PosicionFinal;
import com.g2l.speedg2l.sonidos.EfectoSonido;
import com.g2l.speedg2l.utilidades.Config;
import com.g2l.speedg2l.utilidades.Recursos;
import com.g2l.speedg2l.utilidades.Render;

public class PantallaFin extends Pantalla {

    private Stage stage;
    private PanelBotones panelBotones;
    private SpriteBatch b;

    private EfectoSonido efectoMeta;
    private Texto textoResultados;
    private PosicionFinal[] posiciones;

    public PantallaFin(Game juego, PosicionFinal[] posiciones) {
        super(juego);
        this.posiciones = posiciones;
    }

    @Override
    public void show() {
        b = Render.batch;

        stage = new Stage(configViewport.getViewport());

        textoResultados = new Texto(
            Recursos.FUENTE_MENU,
            35,
            Color.GREEN
        );

        actualizarResultados(posiciones);

        efectoMeta = new EfectoSonido(Recursos.EFECTO_META);

        if (Config.isSonidoSilenciado()) {
            efectoMeta.setVolumen(0.0f);
        } else {
            efectoMeta.setVolumen(Config.getVolumenMaster());
            efectoMeta.reproducir();
        }

        Skin skin = new Skin(
            Gdx.files.internal("skin/uiskin.json")
        );

        crearPanelBotones(skin);
        agregarAlStage();
        configurarInput(stage);
    }

    private void actualizarResultados(PosicionFinal[] nuevosResultados) {
        posiciones = nuevosResultados;

        StringBuilder resultado = new StringBuilder("RESULTADOS\n\n");

        if (posiciones != null) {
            for (int i = 0; i < posiciones.length; i++) {
                PosicionFinal posicion = posiciones[i];

                if (posicion == null) {
                    resultado.append(i + 1)
                        .append("° puesto: En carrera\n");
                } else {
                    resultado.append(posicion.getNumeroPosicion())
                        .append("° puesto: Jugador ")
                        .append(posicion.getIdJugador())
                        .append(" - ")
                        .append(String.format("%.2f s", posicion.getTiempo()))
                        .append("\n");
                }
            }
        }

        textoResultados.setTexto(resultado.toString());
        textoResultados.centrar();
    }

    private void crearPanelBotones(Skin skin) {
        panelBotones = new PanelBotones();

        final int PADDING = 10;
        final int ANCHO_BOTONES = 200;
        final int ALTURA_BOTONES = 60;

        panelBotones.agregarBoton(
            new Boton(
                "Menu",
                skin,
                new ClickListener() {
                    @Override
                    public void clicked(
                        com.badlogic.gdx.scenes.scene2d.InputEvent event,
                        float x,
                        float y
                    ) {
                        cambiarPantalla(new PantallaMenu(juego));
                    }
                },
                ANCHO_BOTONES,
                ALTURA_BOTONES
            ),
            PADDING
        );
    }

    @Override
    public void render(float delta) {
        Render.limpiarPantalla();

        configViewport.aplicarViewport(b);

        b.begin();
        textoResultados.dibujar();
        b.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
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
        if (stage != null) {
            stage.dispose();
        }

        if (efectoMeta != null) {
            efectoMeta.cerrar();
        }
    }

    private void agregarAlStage() {
        stage.addActor(panelBotones.getContenedor());
    }
}
