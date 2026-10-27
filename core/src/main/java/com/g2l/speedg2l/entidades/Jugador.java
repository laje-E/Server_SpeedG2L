package com.g2l.speedg2l.entidades;

import com.badlogic.gdx.math.Rectangle;
import com.g2l.speedg2l.animaciones.AnimacionEntidad;
import com.g2l.speedg2l.utilidades.Direccion;
import com.g2l.speedg2l.utilidades.Entradas;

import java.util.ArrayList;

public class Jugador extends Entidad {

    private final double VELOCIDAD_X = 1;
    private final double VELOCIDAD_Y = 500;

    private int posicionSuelo = 0;

    private double gravedad = 800;
    private double velocidadYMenosGravedad = 0;

    private double velocidadConAceleracionDerecha = VELOCIDAD_X;
    private double velocidadConAceleracionIzquierda = VELOCIDAD_X;
    private double aceleracion = 80;
    private boolean acelerandoDerecha = false;
    private boolean acelerandoIzquierda = false;
    private final double VELOCIDAD_MAXIMA = 1000;
    private AnimacionEntidad animacion;
    boolean saltando = false;

    private boolean rebotando = false;

    private double velocidadReboteX = 0;
    private double velocidadReboteY = 0;

    private final double FUERZA_REBOTE_Y = 450;
    private final double VELOCIDAD_MINIMA_X = 120;
    private final double PERDIDA_VELOCIDAD = 0.75;


    private final float DURACION_EFECTO_LAVA = 0.5f;
    private final double REBOTE_LAVA = 350;
    private final float DURACION_RALENTIZACION = 5.0f;
    private final double VELOCIDAD_MAX_RALENTIZADO = 100;
    private float tiempoRalentizacion = 0;
    private float tiempoEfectoLava = 0;

    private double limiteVelocidad = 0;

    public Jugador(float ancho , float alto, float posicionX, float posicionY, String ruta){
        super(ancho, alto, posicionX, posicionY);
        animacion = new AnimacionEntidad(ruta, 0.09f, 4, 3);
        animacion.iniciar();
    }

    public void animar(float delta) {
        if (acelerandoDerecha || acelerandoIzquierda) {
            animacion.animar(delta);
        }
    }

    public void dibujar() {
        animacion.dibujar(
            posicionX,
            posicionY,
            getAncho(),
            getAlto()
        );
    }

    public void cerrar() {
        animacion.cerrar();
    }

    public void moverIzquierda(boolean presionado) {
        acelerandoIzquierda = presionado;
    }

    public void moverDerecha(boolean presionado) {
        acelerandoDerecha = presionado;
    }

    public void saltar() {
        if (!saltando) {
            velocidadYMenosGravedad = VELOCIDAD_Y;
            saltando = true;
        }
    }

    public void actualizarFisicas(ArrayList<Entidad> listaDeEntidades, float delta) {

        actualizarTemporizadorLava(delta);

        actualizarMovimientoVertical(listaDeEntidades, delta);

        asignarLimiteVelocidad();

        if (rebotando) {
//            actualizarRebote(listaDeEntidades, delta);
            return;
        }

        if (acelerandoDerecha) {
            acelerarDerecha(listaDeEntidades, delta);
        } else {
            desAcelerarDerecha(listaDeEntidades, delta);
        }

        if (acelerandoIzquierda) {
            acelerarIzquierda(listaDeEntidades, delta);
        } else {
            desAcelerarIzquierda(listaDeEntidades, delta);
        }
    }

    private void asignarLimiteVelocidad() {
        if(tiempoEfectoLava > 0){
            limiteVelocidad = VELOCIDAD_MAX_RALENTIZADO;
        }
        else{
            limiteVelocidad = VELOCIDAD_MAXIMA;
        }
    }

    private void actualizarMovimientoVertical(ArrayList<Entidad> listaDeEntidades, float delta) {

        velocidadYMenosGravedad -= gravedad * delta;

        double desplazamientoY = velocidadYMenosGravedad * delta;

        Entidad entidadColisionada =
            hayColisionVertical(listaDeEntidades, desplazamientoY);

        if (entidadColisionada instanceof Plataforma) {

            if (desplazamientoY < 0) {
                // Está cayendo sobre la plataforma

                posicionY = entidadColisionada.getPosicionY()
                    + entidadColisionada.getAlto();

                velocidadYMenosGravedad = 0;
                saltando = false;
                rebotando = false;
            } else {
            }
                // Está subiendo y golpeó la parte inferior

                velocidadYMenosGravedad = 0;
            }

        if (entidadColisionada == null) {
            // No hay plataforma: continúa moviéndose
            posicionY += desplazamientoY;
            saltando = true;
        }


        if (entidadColisionada instanceof Lava) {
            aplicarEfectoLava();
        }

        // Evitar atravesar el suelo
        if (posicionY <= posicionSuelo) {
            posicionY = posicionSuelo;
            velocidadYMenosGravedad = 0;
            saltando = false;
        }

        actualizarHitbox();
    }


//<<<<<<< HEAD
//    public void iniciarRebote() {
//        if (rebotando) {
//            return;
//        }
//
//        rebotando = true;
//
//        // Bloquear los controles.
//        acelerandoDerecha = false;
//        acelerandoIzquierda = false;
//
//        // Determinar la dirección anterior.
//        if (velocidadConAceleracionDerecha >
//            velocidadConAceleracionIzquierda) {
//
//            velocidadReboteX =
//                Math.max(velocidadConAceleracionDerecha,
//                    VELOCIDAD_MINIMA_X);
//
//        } else if (velocidadConAceleracionIzquierda >
//            velocidadConAceleracionDerecha) {
//
//            velocidadReboteX =
//                -Math.max(velocidadConAceleracionIzquierda,
//                    VELOCIDAD_MINIMA_X);
//
//        } else {
//            // Si estaba quieto, elegir una dirección inicial.
//            velocidadReboteX = VELOCIDAD_MINIMA_X;
//        }
//
//        velocidadReboteY = FUERZA_REBOTE_Y;
//
//        velocidadConAceleracionDerecha = 0;
//        velocidadConAceleracionIzquierda = 0;
//
//        saltando = true;
//    }
//
//    private void rebotar() {
//
//        // Reducir la velocidad horizontal sin invertir la dirección.
//        double direccion = Math.signum(velocidadReboteX);
//
//        velocidadReboteX =
//            direccion * Math.max(
//                Math.abs(velocidadReboteX) * PERDIDA_VELOCIDAD,
//                VELOCIDAD_MINIMA_X
//            );
//
//        // Volver a impulsarlo hacia arriba con menos fuerza.
//        velocidadReboteY = Math.max(
//            Math.abs(velocidadReboteY) * PERDIDA_VELOCIDAD,
//            180
//        );
//    }
//
//    private void actualizarRebote(
//        ArrayList<Entidad> listaDeEntidades,
//        float delta
//    ) {
//
//        rebotar();
//
//        // Mover horizontalmente.
//        double desplazamientoX = velocidadReboteX * delta;
//
//        Entidad entidadX =
//            hayColisionHorizontal(listaDeEntidades, desplazamientoX);
//
//        if (entidadX == null) {
//            posicionX += desplazamientoX;
//        } else if (entidadX instanceof Plataforma) {
//            velocidadReboteX = 0;
//        }
//
//        // Mover verticalmente.
//        double desplazamientoY = velocidadReboteY * delta;
//
//        Entidad entidadY =
//            hayColisionVertical(listaDeEntidades, desplazamientoY);
//
//        if (entidadY instanceof Plataforma) {
//            if (desplazamientoY < 0) {
//                posicionY = entidadY.getPosicionY()
//                    + entidadY.getAlto();
//
//                velocidadReboteY = 0;
//                rebotando = false;
//            } else {
//                velocidadReboteY = 0;
//            }
//        } else if (entidadY == null) {
//            posicionY += desplazamientoY;
//        }
//
//        // Mantener al jugador dentro del suelo.
//        if (posicionY <= posicionSuelo) {
//            posicionY = posicionSuelo;
//
//            if (Math.abs(velocidadReboteY) < 180) {
//                velocidadReboteY = 180;
//            } else {
//                velocidadReboteY = 0;
//            }
//        }
//
//        actualizarHitbox();
//
//    }
//
//    protected void variacionVelocidad(double multiplicadorDeVariacion) {
//        if (velocidadConAceleracionDerecha > 0) {
//            velocidadConAceleracionDerecha *= multiplicadorDeVariacion;
//        } else if (velocidadConAceleracionIzquierda > 0) {
//=======
    protected void rebotar(ArrayList<Entidad> listaDeEntidades, float delta){
        actualizarMovimientoVertical(listaDeEntidades, delta);
        if (velocidadConAceleracionDerecha > 5){
            variacionVelocidad(0.5);
        }
        else if (velocidadConAceleracionDerecha > 0){
            velocidadConAceleracionDerecha = 2.5;
        }
        else if (velocidadConAceleracionIzquierda > 5){
            variacionVelocidad(0.5);
        }
        else if (velocidadConAceleracionIzquierda > 0){
            velocidadConAceleracionIzquierda = 5;
        }
        else if (velocidadConAceleracionIzquierda == 0 && velocidadConAceleracionDerecha == 0){
            velocidadConAceleracionDerecha = 2.5;
        }
    }

    protected void variacionVelocidad(double multiplicadorDeVariacion){
        if (velocidadConAceleracionDerecha > 0){
            velocidadConAceleracionDerecha *= multiplicadorDeVariacion;
        }
        else if (velocidadConAceleracionIzquierda > 0){
            velocidadConAceleracionIzquierda *= multiplicadorDeVariacion;
        }
    }

    private Entidad hayColisionVertical(ArrayList<Entidad> listaDeEntidades, double aceleracion) {

        boolean hayColision = false;
        Entidad entidadColisionada = null;

        Rectangle futuraHitbox = new Rectangle(
            (float) getPosicionX(),
            (float) getPosicionY() + (float) aceleracion,
            getAncho(),
            getAlto()
        );

        int i = 0;

        while (i < listaDeEntidades.size() && !hayColision) {

            if (futuraHitbox.overlaps(listaDeEntidades.get(i).getHitbox())) {
                hayColision = true;
                entidadColisionada = listaDeEntidades.get(i);
            }

            i++;
        }

        return entidadColisionada;
    }

    private void acelerarDerecha(ArrayList<Entidad> listaDeEntidades, float delta) {
        if (velocidadConAceleracionDerecha < limiteVelocidad) {
            this.velocidadConAceleracionDerecha += aceleracion * delta;
            if (velocidadConAceleracionDerecha > limiteVelocidad) {
                velocidadConAceleracionDerecha = limiteVelocidad;
            }
        }
        double desplazamientoX = velocidadConAceleracionDerecha * delta;

        Entidad entidadColisionada = hayColisionHorizontal(listaDeEntidades, desplazamientoX);
        trabajarColisionHorizontal(entidadColisionada, Direccion.DERECHA, desplazamientoX);
        actualizarHitbox();
        // System.out.println("La aceleracion del jugador es de: " + velocidadConAceleracionDerecha);
    }

    private void desAcelerarDerecha(ArrayList<Entidad> listaDeEntidades, float delta) {
        if (velocidadConAceleracionDerecha > 0) {
            velocidadConAceleracionDerecha -= aceleracion * delta;
            if (velocidadConAceleracionDerecha < 0) {
                velocidadConAceleracionDerecha = 0;
            }
        }
        double desplazamientoX = velocidadConAceleracionDerecha * delta;

        Entidad entidadColisionada = hayColisionHorizontal(listaDeEntidades, desplazamientoX);
        trabajarColisionHorizontal(entidadColisionada, Direccion.DERECHA, desplazamientoX);
        actualizarHitbox();
    }

    private void acelerarIzquierda(ArrayList<Entidad> listaDeEntidades, float delta) {
        if (velocidadConAceleracionIzquierda < limiteVelocidad) {
            velocidadConAceleracionIzquierda += aceleracion * delta;
            if (velocidadConAceleracionIzquierda > limiteVelocidad) {
                velocidadConAceleracionIzquierda = limiteVelocidad;
            }
        }
        double desplazamientoX = velocidadConAceleracionIzquierda * delta;

        Entidad entidadColisionada = hayColisionHorizontal(listaDeEntidades, -desplazamientoX);
        trabajarColisionHorizontal(entidadColisionada, Direccion.IZQUIERDA, desplazamientoX);
        actualizarHitbox();
    }

    private void desAcelerarIzquierda(ArrayList<Entidad> listaDeEntidades, float delta) {
        if (velocidadConAceleracionIzquierda > 0) {
            velocidadConAceleracionIzquierda -= aceleracion * delta;
            if (velocidadConAceleracionIzquierda < 0) {
                velocidadConAceleracionIzquierda = 0;
            }
        }
        double desplazamientoX = velocidadConAceleracionIzquierda * delta;

        Entidad entidadColisionada = hayColisionHorizontal(listaDeEntidades, -desplazamientoX);
        trabajarColisionHorizontal(entidadColisionada, Direccion.IZQUIERDA, desplazamientoX);
        actualizarHitbox();
    }

    private Entidad hayColisionHorizontal(ArrayList<Entidad> listaDeEntidades, double aceleracion) {

        boolean hayColision = false;
        Entidad entidadColisionada = null;

        Rectangle futuraHitbox = new Rectangle(
            getPosicionX() + (float) aceleracion,
            getPosicionY(),
            getAncho(),
            getAlto()
        );

        int i = 0;

        while (i < listaDeEntidades.size() && !hayColision) {

            if (futuraHitbox.overlaps(listaDeEntidades.get(i).getHitbox())) {
                hayColision = true;
                entidadColisionada = listaDeEntidades.get(i);
            }

            i++;
        }

        return entidadColisionada;
    }


    private void trabajarColisionHorizontal(Entidad entidadColisionada, Direccion direccionMovimiento, double desplazamientoX) {

        switch (direccionMovimiento) {
            case DERECHA:
                if (entidadColisionada == null) {
                    posicionX += desplazamientoX;
                } else if (entidadColisionada instanceof Plataforma) {
                    velocidadConAceleracionDerecha = 0;
                } else if (entidadColisionada instanceof Obstaculo) {
                    Obstaculo obstaculoColisionado = (Obstaculo) entidadColisionada;
                    if (obstaculoColisionado.getColisionLateral()) {
                        obstaculoColisionado.alColisionar(this);
                    }
                }

                break;


            case IZQUIERDA:
                if (entidadColisionada == null) {
                    posicionX -= desplazamientoX;
                } else if (entidadColisionada instanceof Plataforma) {
                    velocidadConAceleracionIzquierda = 0;
                } else if (entidadColisionada instanceof Obstaculo) {
                    Obstaculo obstaculoColisionado = (Obstaculo) entidadColisionada;
                    if (obstaculoColisionado.getColisionLateral()) {
                        obstaculoColisionado.alColisionar(this);
                    }
                }
        }
    }

    public boolean colisionaCon(Entidad entidad) {
        return getHitbox().overlaps(entidad.getHitbox());
    }


    public void aplicarEfectoLava() {
        tiempoRalentizacion = DURACION_RALENTIZACION;
        velocidadYMenosGravedad = REBOTE_LAVA;
        saltando = true;
        tiempoEfectoLava = DURACION_EFECTO_LAVA;
    }

    private void actualizarTemporizadorLava(float delta) {
        if (tiempoEfectoLava > 0) tiempoEfectoLava -= delta;
    }
}
