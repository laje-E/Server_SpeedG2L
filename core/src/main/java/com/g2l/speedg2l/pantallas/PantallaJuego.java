package com.g2l.speedg2l.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.g2l.speedg2l.componentes.Texto;
import com.g2l.speedg2l.componentes.interfaz.Hud;
import com.g2l.speedg2l.entidades.*;
import com.g2l.speedg2l.mundo.Camara;
import com.g2l.speedg2l.mundo.Mapa;
import com.g2l.speedg2l.servidor.HiloServer;
import com.g2l.speedg2l.servidor.PosicionFinal;
import com.g2l.speedg2l.servidor.ProtocolosRed;
import com.g2l.speedg2l.sonidos.Musica;
import com.g2l.speedg2l.utilidades.Config;
import com.g2l.speedg2l.utilidades.Recursos;

import java.util.ArrayList;

public class PantallaJuego extends Pantalla {

    private Stage stage;

    private Jugador jugador;
    private Jugador jugador2;
    private Musica musicaJuego;
    private Hud hud;

    private ArrayList<Entidad> listaDeEntidades;

    private Mapa mapa;
    private Camara camara;
    private Texto textoEspera = new Texto(Recursos.FUENTE_MENU, 60, Color.WHITE, batch);

    private Meta meta;

    public static boolean empieza = false;

    private HiloServer hiloServidor;

    private PosicionFinal[] posiciones;
    private boolean[] jugadorTermino = new boolean[2];
    private int contadorPosicion = 1;

    private boolean terminarJuego = false;

    protected PantallaJuego(Game juego, SpriteBatch b) {
        super(juego, b);
    }

    @Override
    public void show() {
        empieza = false;
        contadorPosicion = 1;
        jugadorTermino = new boolean[2];
        posiciones = new PosicionFinal[2];
        crearYaplicarMusica();

        camara = new Camara(configViewport);
        mapa = new Mapa(Recursos.NIVEL_1);
        jugador = Jugadores.JUGADOR_1.crearJugador(batch);
        jugador2 = Jugadores.JUGADOR_2.crearJugador(batch);
        hud = new Hud(batch);

        meta = new Meta(50.0f, 1000.0f, 8000.0f, 100.0f);

        listaDeEntidades = new ArrayList<>();

        textoEspera.setTexto("Esperando jugador");
        textoEspera.centrar();

        cargarColisionesDesdeMapa();

        stage = new Stage(
            new FitViewport(
                Config.getAnchoJuego(),
                Config.getAltoJuego()
            )
        );

        crearYConfigurarHiloServer();
        hiloServidor.start();

    }

    private void crearYConfigurarHiloServer() {
        hiloServidor = new HiloServer();
        hiloServidor.almacenarJugadores(jugador, jugador2);
    }

    private void crearYaplicarMusica() {
        musicaJuego = new Musica(Recursos.MUSICA_JUEGO);
        musicaJuego.repetir(true);

        if (Config.isSonidoSilenciado()) {
            musicaJuego.setVolumen(0.0f);
        } else {
            musicaJuego.setVolumen(Config.getVolumenMaster());
        }
        musicaJuego.reproducir();
    }

    @Override
    public void render(float delta) {
        limpiarPantalla();

        hiloServidor.actualizarCuentaRegresiva();

        if(hiloServidor.getCantClientes() < 2){
            batch.begin();

            textoEspera.dibujar();

            batch.end();
        }

        if (hiloServidor.isFin() || hiloServidor.isClienteDesconectado() || (hiloServidor.isFinSesionSolicitado() && terminarJuego)) {
            hiloServidor.finalizarHilo();
            cambiarPantalla(new PantallaMenu(juego, batch));
            return;
        }

        if (hiloServidor.isReinicioSolicitado()) {

            reiniciarPartida();

            hiloServidor.confirmarReinicio();
            hiloServidor.reiniciarPartida();
        }

        if(empieza) {
            delta = Gdx.graphics.getDeltaTime();

            limpiarPantalla();

            camara.seguirJugador(jugador);

            mapa.dibujar(camara.getCamara());

            batch.setProjectionMatrix(camara.getCamara().combined);
            batch.begin();

                  jugador.actualizarFisicas(listaDeEntidades, delta);

                  jugador2.actualizarFisicas(listaDeEntidades, delta);
                  hiloServidor.realizarBroadcastJuego();

                hud.actualizar();
                if (!jugadorTermino[0] && jugador.colisionaCon(meta)) {
                    registrarLlegada(1);
                }
                if (!jugadorTermino[1] && jugador2.colisionaCon(meta)) {
                    registrarLlegada(2);
                }
                if (jugadorTermino[0] && jugadorTermino[1]) {
                    terminarJuego();
                    terminarJuego = true;

                    hiloServidor.marcarPartidaFinalizada();
                }


            jugador.dibujar();
            jugador2.dibujar();

            batch.end();

            batch.setProjectionMatrix(stage.getCamera().combined);


            batch.begin();

            hud.dibujar();

            batch.end();

            stage.act(delta);
            stage.draw();

        }
    }


    private void cargarColisionesDesdeMapa() {
        if (mapa != null && mapa.getMapa() != null) {
            TiledMapTileLayer capa = (TiledMapTileLayer) mapa.getMapa().getLayers().get("Capa de patrones 1");
            if (capa != null) {
                int tileAncho = (int) capa.getTileWidth();
                int tileAlto = (int) capa.getTileHeight();

                for (int x = 0; x < capa.getWidth(); x++) {
                    for (int y = 0; y < capa.getHeight(); y++) {
                        TiledMapTileLayer.Cell cell = capa.getCell(x, y);
                        if (cell != null && cell.getTile() != null) {
                            MapProperties propiedades = cell.getTile().getProperties();

                            if (propiedades.containsKey("solido") && propiedades.get("solido", Boolean.class)) {
                                Plataforma bloque = new Plataforma(tileAncho, tileAlto, x * tileAncho, y * tileAlto);
                                listaDeEntidades.add(bloque);
                            }

                            if (propiedades.containsKey("mortal") && propiedades.get("mortal", Boolean.class)) {
                                Lava lava = new Lava(tileAncho, tileAlto, x * tileAncho, y * tileAlto);
                                listaDeEntidades.add(lava);
                            }
                        }
                    }
                }
            }
        }
    }

    private void registrarLlegada(int idJugador) {
        int indice = idJugador - 1;

        if (!jugadorTermino[indice]) {
            jugadorTermino[indice] = true;
            hiloServidor.setJugadorTerminado(indice);

            posiciones[contadorPosicion - 1] = new PosicionFinal(hud.getTiempo(), idJugador,  contadorPosicion);

            hiloServidor.enviarMensajeAtodos(
                ProtocolosRed.LLEGO +
                ProtocolosRed.SEPARADOR +
                idJugador +
                ProtocolosRed.SEPARADOR +
                contadorPosicion +
                ProtocolosRed.SEPARADOR +
                hud.getTiempo()
            );

            contadorPosicion++;

            if (contadorPosicion > 2) {
                hiloServidor.enviarMensajeAtodos(ProtocolosRed.TODOS_TERMINARON);
            }
        }
    }

    private void reiniciarPartida() {

        empieza = false;
        terminarJuego = false;

        contadorPosicion = 1;

        jugadorTermino = new boolean[2];
        posiciones = new PosicionFinal[2];

        // Reiniciar jugadores.
        jugador.reiniciar();
        jugador2.reiniciar();

        // Reiniciar cronómetro del HUD.
        hud.reiniciar();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        if (hiloServidor != null) {
            hiloServidor.finalizarHilo();
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }

        if (musicaJuego != null) {
            musicaJuego.cerrar();
        }

        if (jugador != null) {
            jugador.cerrar();
        }

        if (jugador2 != null) {
            jugador2.cerrar();
        }

        if (mapa != null) {
            mapa.cerrar();
        }

        if (hud != null) {
            hud.cerrar();
        }

        if (textoEspera != null) {
            textoEspera.cerrar();
        }
    }

    public static void empezarJuego(){
        empieza = true;
    }

    public void terminarJuego(){
        empieza = false;
    }

    public Jugador getJugador(Jugadores jugadorElegido) {
        if (jugadorElegido.equals(Jugadores.JUGADOR_1)) {
            return jugador;
        }
        else{
            return jugador2;
        }
    }

}
