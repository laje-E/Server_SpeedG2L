package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.entidades.Entidad;
import com.g2l.speedg2l.entidades.Jugador;
import com.g2l.speedg2l.entidades.Jugadores;
import com.g2l.speedg2l.pantallas.PantallaJuego;
import com.g2l.speedg2l.utilidades.Entradas;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HiloServer extends Thread{

    private InetAddress direccionServer;
    private DatagramSocket puertoServer;
    private boolean fin = false;
    private DireccionRed [] direccionesClientes = new DireccionRed[2];
    private int cantClientes = 0;

    private Jugador[] jugadores = new Jugador[2];

    private Entradas entradas = new Entradas();

    private ArrayList<Entidad> listaEntidades = new ArrayList<>();
    private float delta;

    public HiloServer(){
        try {
            direccionServer = InetAddress.getByName("255.255.255.255");
            puertoServer = new DatagramSocket(6412);
        }catch (SocketException | UnknownHostException evento){
            evento.printStackTrace();
        }
    }

    public void enviarMensaje(String mensaje, InetAddress ipDestino, int puerto){
        byte[] data = mensaje.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ipDestino, puerto);
        try {
//            System.out.println("Mensaje: " + (new String (dp.getData())).trim());
            puertoServer.send(dp);
        }catch (IOException event){
            event.printStackTrace();
        }
    }

    public void enviarMensajeATodos(String mensaje){
        for(int i=0; i<cantClientes; i++){
            byte[] data = mensaje.getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, direccionesClientes[i].getIp(), direccionesClientes[i].getPuerto());
            try {
//                System.out.println("Mensaje: " + (new String (dp.getData())).trim());
                puertoServer.send(dp);
            }catch (IOException event){
                event.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        System.out.println("Servidor iniciado. Esperando conexiones...");

        do {
            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);

            try {
                puertoServer.receive(dp);

//                System.out.println("Recibí un paquete de: "
//                    + dp.getAddress()
//                    + ":"
//                    + dp.getPort());

            } catch (IOException event) {
                event.printStackTrace();
            }

            procesarMensaje(dp);

            jugadores[0].actualizarFisicas(listaEntidades, delta);
            jugadores[1].actualizarFisicas(listaEntidades, delta);

        } while(!fin);
    }

//    private void actualizarFisicasJugadores() {
//        for(int i=0; i<jugadores.length; i++){
//            jugadores[i].actualizarFisicas();
//        }
//    }

    private void procesarMensaje(DatagramPacket dp){
        String mensaje = (new String (dp.getData())).trim();
//      System.out.println("mensaje cliente: " + mensaje);
        if(mensaje.equals("Conexion")){
            if(cantClientes < 2) {
                if (cantClientes == 0) {
                    direccionesClientes[0] = new DireccionRed(dp.getAddress(), dp.getPort());
                    System.out.println("Datos cliente nro°" + cantClientes + ": " + direccionesClientes[cantClientes].getIp() + direccionesClientes[cantClientes].getPuerto());
                    enviarMensaje("OK", direccionesClientes[0].getIp(), direccionesClientes[0].getPuerto());
                    cantClientes++;
                } else if (cantClientes == 1) {
                    direccionesClientes[1] = new DireccionRed(dp.getAddress(), dp.getPort());
                    enviarMensaje("OK", direccionesClientes[1].getIp(), direccionesClientes[1].getPuerto());
                    cantClientes++;
                }
                if (cantClientes == 1){
                    for (int i=0; i<cantClientes; i++){
                        enviarMensaje("Empezar", direccionesClientes[i].getIp(), direccionesClientes[i].getPuerto());
                        PantallaJuego.empezarJuego();
                    }
                }
            }
            else{
                enviarMensaje("ERROR-limiteDeClientesAlcanzado", dp.getAddress(), dp.getPort());
            }
        }
        String[] mensajePorPartes = mensaje.split("-");
        int numeroCliente = detectarCliente(dp);
            if (mensajePorPartes[0].equals("Aprete")) {
                System.out.println(mensaje);
                if (mensajePorPartes[1].equals("Izquierda")) {

                    jugadores[numeroCliente].moverIzquierda(true);
                    enviarMensajeATodos("Movimiento-" + jugadores[numeroCliente].getPosicionX() + "-" +
                                         jugadores[numeroCliente].getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                } else if (mensajePorPartes[1].equals("Derecha")) {

                    jugadores[numeroCliente].moverDerecha(true);
                    enviarMensajeATodos("Movimiento-" + jugadores[numeroCliente].getPosicionX() + "-" +
                                         jugadores[numeroCliente].getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                } else if (mensajePorPartes[1].equals("Arriba")) {

                    jugadores[numeroCliente].saltar();
                    enviarMensajeATodos("Movimiento-" + jugadores[numeroCliente].getPosicionX() + "-" +
                        jugadores[numeroCliente].getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                }
            } else if (mensajePorPartes[0].equals("NoAprete")) {
                if (mensajePorPartes[1].equals("Izquierda")) {

                    jugadores[numeroCliente].moverIzquierda(false);
                    enviarMensajeATodos("Movimiento-" + jugadores[numeroCliente].getPosicionX() + "-" +
                        jugadores[numeroCliente].getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                } else if (mensajePorPartes[1].equals("Derecha")) {

                    jugadores[numeroCliente].moverDerecha(false);
                    enviarMensajeATodos("Movimiento-" + jugadores[numeroCliente].getPosicionX() + "-" +
                        jugadores[numeroCliente].getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                }
            }

    }

    private int detectarCliente(DatagramPacket dp) {
        int numeroCliente = -1;
        int i=0;
        while(i<direccionesClientes.length && numeroCliente == -1){
            if (dp.getAddress().equals(direccionesClientes[i].getIp()) && dp.getPort() == direccionesClientes[i].getPuerto()){
                numeroCliente = i;
            }
            i++;
        }
        return numeroCliente;
    }

    public int getCantClientes() {
        return cantClientes;
    }

    public void almacenarJugadores(Jugador jugador, Jugador jugador2) {
        jugadores[0] = jugador;
        jugadores[1] = jugador2;
    }

    public void almacenarListaEntidades(ArrayList<Entidad> listaDeEntidades) {
        this.listaEntidades = listaDeEntidades;
    }

    public void almacenarDelta(float delta) {
        this.delta = delta;
    }
}
