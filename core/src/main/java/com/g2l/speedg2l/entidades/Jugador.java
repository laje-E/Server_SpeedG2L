package com.g2l.speedg2l.entidades;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    private final float DURACION_EFECTO_LAVA = 0.5f;
    private final double REBOTE_LAVA = 350;
    private float tiempoRalentizacion = 0;
    private float tiempoEfectoLava = 0;

    private double limiteVelocidad = 0;

    private final float POSICION_INICIAL_X = 0;
    private final float POSICION_INICIAL_Y = 0;

    public Jugador(float ancho , float alto, float posicionX, float posicionY, SpriteBatch batch, String ruta){
        super(ancho, alto, posicionX, posicionY);
        animacion = new AnimacionEntidad(ruta, 0.09f, 4, 3, batch);
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


    public void reiniciar() {

        // 1. Restablecer posición.
        posicionX = POSICION_INICIAL_X;
        posicionY = POSICION_INICIAL_Y;

        // 2. Restablecer velocidades horizontales.
        velocidadConAceleracionDerecha = VELOCIDAD_X;
        velocidadConAceleracionIzquierda = VELOCIDAD_X;

        // 3. Restablecer movimiento vertical.
        velocidadYMenosGravedad = 0;

        // 4. Desactivar controles.
        acelerandoDerecha = false;
        acelerandoIzquierda = false;

        // 5. Restablecer estados del jugador.
        saltando = false;
        rebotando = false;

        // 6. Restablecer efectos de lava.
        tiempoRalentizacion = 0;
        tiempoEfectoLava = 0;

        // 7. Restablecer límite de velocidad.
        limiteVelocidad = VELOCIDAD_MAXIMA;

        // 8. Actualizar hitbox.
        actualizarHitbox();
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
        if (tiempoRalentizacion > 0) {
            double VELOCIDAD_MAX_RALENTIZADO = 100;
            limiteVelocidad = VELOCIDAD_MAX_RALENTIZADO;
        } else {
            limiteVelocidad = VELOCIDAD_MAXIMA;
        }

        // Recortar la velocidad actual si supera el límite
        if (velocidadConAceleracionDerecha > limiteVelocidad) {
            velocidadConAceleracionDerecha = limiteVelocidad;
        }
        if (velocidadConAceleracionIzquierda > limiteVelocidad) {
            velocidadConAceleracionIzquierda = limiteVelocidad;
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
        float duracion_Ralentizacion = 3.0f;
        tiempoRalentizacion = duracion_Ralentizacion;

        if (tiempoEfectoLava > 0) {
            return;
        }
        velocidadYMenosGravedad = REBOTE_LAVA;
        saltando = true;
        tiempoEfectoLava = DURACION_EFECTO_LAVA;
    }

    private void actualizarTemporizadorLava(float delta) {
        if (tiempoEfectoLava > 0) tiempoEfectoLava -= delta;
        if (tiempoRalentizacion > 0) tiempoRalentizacion -= delta;
    }
}
