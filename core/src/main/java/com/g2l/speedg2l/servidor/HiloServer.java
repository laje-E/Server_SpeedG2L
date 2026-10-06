package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.entidades.Entidad;
import com.g2l.speedg2l.entidades.Jugador;
import com.g2l.speedg2l.entidades.Jugadores;
import com.g2l.speedg2l.pantallas.PantallaJuego;
import com.g2l.speedg2l.utilidades.Direccion;
import com.g2l.speedg2l.utilidades.Entradas;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HiloServer extends Thread{

    private InetAddress direccionServer;
    private DatagramSocket puertoServer;
    private boolean fin=false;

    private Cliente[] clientes;
    private int cantClientes=0;
    private Jugador jugador1, jugador2;
    private ArrayList<Entidad> listaEntidades;

    private float delta;

    public HiloServer(){
        clientes = new Cliente[2];
        listaEntidades = new ArrayList<>();
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
            DireccionRed direccionCliente = clientes[i].getDireccionRed();
            DatagramPacket dp = new DatagramPacket(data, data.length, direccionCliente.getIp(), direccionCliente.getPuerto());
            try {
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
                jugador1.actualizarFisicas(listaEntidades, delta);
                jugador2.actualizarFisicas(listaEntidades, delta);

            } catch (IOException event) {
                event.printStackTrace();
            }

            procesarMensaje(dp);

        } while(!fin);
    }

//    private void actualizarFisicasJugadores() {
//        for(int i=0; i<jugadores.length; i++){
//            jugadores[i].actualizarFisicas();
//        }
//    }

    private void procesarMensaje(DatagramPacket dp){
        String mensaje = (new String (dp.getData())).trim();
        InetAddress ipPaquete = dp.getAddress();
        int puertoPaquete = dp.getPort();

        evaluarMensaje(mensaje, ipPaquete, puertoPaquete);
    }

    private void evaluarMensaje(String mensaje, InetAddress ipPaquete, int puertoPaquete){
        if(mensaje.equals("Conexion")){
            if(cantClientes < 2) {
                DireccionRed direccionRedParaCliente = new DireccionRed(ipPaquete, puertoPaquete);
                if (cantClientes == 0) {
                    clientes[0] = new Cliente(direccionRedParaCliente, jugador1);
                    System.out.println("Datos cliente nro°" + cantClientes + ": " + direccionRedParaCliente.getIp() + direccionRedParaCliente.getPuerto());
                    enviarMensaje("OK", direccionRedParaCliente.getIp(), direccionRedParaCliente.getPuerto());
                    cantClientes++;
                } else if (cantClientes == 1) {
                    clientes[1] = new Cliente(direccionRedParaCliente, jugador2);
                    enviarMensaje("OK", direccionRedParaCliente.getIp(), direccionRedParaCliente.getPuerto());
                    cantClientes++;
                }
                if (cantClientes == 2){
                    for (int i=0; i<cantClientes; i++){
                        DireccionRed direccionRedCliente = clientes[i].getDireccionRed();
                        enviarMensaje("Empezar", direccionRedCliente.getIp(), direccionRedCliente.getPuerto());
                        PantallaJuego.empezarJuego();
                    }
                }
            }
            else{
                enviarMensaje("ERROR-limiteDeClientesAlcanzado", ipPaquete, puertoPaquete);
            }
        } else{
            String[] mensajePorPartes = mensaje.split("-");
            int numeroCliente = detectarCliente(ipPaquete, puertoPaquete);
            if(numeroCliente == -1){
                System.err.println("Error, cliente no detectado al revisar en el paquete.");
            } else{
                Jugador jugadorCliente = clientes[numeroCliente].getJugador();
                if (mensajePorPartes[0].equals("Aprete")) {
                    System.out.println(mensaje);
                    if (mensajePorPartes[1].equals("Izquierda")) {
                        jugadorCliente.moverIzquierda(true);
                        enviarMensajeATodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Derecha")) {

                        jugadorCliente.moverDerecha(true);
                        enviarMensajeATodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Arriba")) {

                        jugadorCliente.saltar();
                        enviarMensajeATodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    }
                } else if (mensajePorPartes[0].equals("NoAprete")) {
                    if (mensajePorPartes[1].equals("Izquierda")) {

                        jugadorCliente.moverIzquierda(false);
                        enviarMensajeATodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Derecha")) {

                        jugadorCliente.moverDerecha(false);
                        enviarMensajeATodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    }
                }

            }

        }
    }

    private int detectarCliente(InetAddress ipPaquete, int puertoPaquete) {
        int numeroCliente = -1;
        int i=0;
        while(i<cantClientes && numeroCliente == -1){
            DireccionRed direccionClienteActual = clientes[i].getDireccionRed();
            if (ipPaquete.equals(direccionClienteActual.getIp()) && puertoPaquete == direccionClienteActual.getPuerto()){
                numeroCliente = i;
            }
            i++;
        }
        return numeroCliente;
    }

    public int getCantClientes() {
        return cantClientes;
    }

    public void almacenarJugadores(Jugador jugador1, Jugador jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
    }

    public void almacenarListaEntidades(ArrayList<Entidad> listaDeEntidades) {
        this.listaEntidades = listaDeEntidades;
    }

    public void almacenarDelta(float delta) {
        this.delta = delta;
    }
}
