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
//  private ArrayList<Entidad> listaEntidades;

//    private float delta;

    private boolean clienteConfirmado = false;

    private final int  TIEMPO_ESPERA_RECEIVE = 500;
    private final long TIMEOUT_CLIENTE       = 5000;

    private long[] ultimoContacto = new long[2];
    private boolean[] terminado   = new boolean[2];   // jugadores que ya llegaron a la meta

    private volatile boolean partidaEnCurso = false;
    private volatile boolean partidaCancelada = false;

    private boolean clienteDesconectado = false;

    public HiloServer(){
        clientes = new Cliente[2];
//      listaEntidades = new ArrayList<>();
        try {
            direccionServer = InetAddress.getByName("255.255.255.255");
            puertoServer = new DatagramSocket(6412);
//            puertoServer.setSoTimeout(TIEMPO_ESPERA_RECEIVE);
        }catch (SocketException | UnknownHostException evento){
            evento.printStackTrace();
        }
    }

    public boolean isClienteConfirmado() {
        return clienteConfirmado;
    }

    public void enviarMensaje(String mensaje, InetAddress ipDestino, int puerto){
        byte[] data = mensaje.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ipDestino, puerto);
        try {
//            System.out.println("Mensaje: " + (new String (dp.getData())).tr   im());
            puertoServer.send(dp);
        }catch (IOException event){
            event.printStackTrace();
        }
    }

    public void enviarMensajeAtodos(String mensaje){
        byte[] data = mensaje.getBytes();

        for(int i=0; i<clientes.length; i++){
           if(clientes[i] != null) {
                DireccionRed direccionCliente = clientes[i].getDireccionRed();
                DatagramPacket dp = new DatagramPacket(data, data.length, direccionCliente.getIp(), direccionCliente.getPuerto());
                try {
                    puertoServer.send(dp);
                } catch (IOException event) {
                    event.printStackTrace();
                }
            }
        }
    }


    public void realizarBroadcastJuego() {
        for(int i = 0; i < clientes.length; i++) {
            if(clientes[i] != null) {
                Jugador jugadorCliente = clientes[i].getJugador();
                enviarMensajeAtodos("Movimiento-" +
                        jugadorCliente.getPosicionX() + "-" +
                        jugadorCliente.getPosicionY() + "-" +
                        Jugadores.values()[i]
                );
            }
        }
    }

    @Override
    public void run() {
        System.out.println("Servidor iniciado. Esperando conexiones...");

        while(!fin && puertoServer != null && !puertoServer.isClosed()){
            if(puertoServer == null){
                System.err.println("El socket del servidor no está disponible.");
                fin=true;
            }
            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);

            try{
                puertoServer.receive(dp);
                procesarMensaje(dp);
            }catch(IOException event){
                if (puertoServer.isClosed()) {
                    break;
                }
                event.printStackTrace();
                break;
            }

            if (puertoServer.isClosed()){
                break;
            }

            revisarTimeouts();

        }

        System.out.println("\n\nHiloServer finalizado.");
    }

      private void procesarMensaje(DatagramPacket dp){
        String mensaje = (new String (dp.getData())).trim();
        InetAddress ipPaquete = dp.getAddress();
        int puertoPaquete = dp.getPort();

        evaluarMensaje(mensaje, ipPaquete, puertoPaquete);
    }

    private void evaluarMensaje(String mensaje, InetAddress ipPaquete, int puertoPaquete){

        int numeroCliente = detectarCliente(ipPaquete, puertoPaquete);
        if (numeroCliente != -1) {
            ultimoContacto[numeroCliente] = System.currentTimeMillis();
        }

        if(mensaje.equals("Conexion")){
            if(cantClientes < 2) {
                int espacio = buscarEspacioLibre();

                if(espacio != -1) {
                    DireccionRed direccionRedParaCliente = new DireccionRed(ipPaquete, puertoPaquete);
                    if (espacio == 0) {
                        clientes[0] = new Cliente(direccionRedParaCliente, jugador1);
                        ultimoContacto[0] = System.currentTimeMillis();
                        System.out.println("Datos cliente nro°" + cantClientes + ": " + direccionRedParaCliente.getIp() + direccionRedParaCliente.getPuerto());
                        enviarMensaje("OK", direccionRedParaCliente.getIp(), direccionRedParaCliente.getPuerto());
                        cantClientes++;
                    } else if (espacio == 1) {
                        clientes[1] = new Cliente(direccionRedParaCliente, jugador2);
                        ultimoContacto[1] = System.currentTimeMillis();
                        enviarMensaje("OK", direccionRedParaCliente.getIp(), direccionRedParaCliente.getPuerto());
                        cantClientes++;
                    }
                    if (cantClientes == 2) {
                        for (int i = 0; i < cantClientes; i++) {
                            DireccionRed direccionRedCliente = clientes[i].getDireccionRed();
                            enviarMensaje("Empezar", direccionRedCliente.getIp(), direccionRedCliente.getPuerto());
                            enviarMensaje("NumeroJugador-" + Jugadores.values()[i].toString(), direccionRedCliente.getIp(), direccionRedCliente.getPuerto());
                            PantallaJuego.empezarJuego();
                        }
                    }
                }
            }
            else{
                enviarMensaje("ERROR-limiteDeClientesAlcanzado", ipPaquete, puertoPaquete);
            }
        }
            String[] mensajePorPartes = mensaje.split("-");
            if(numeroCliente == -1){
                System.err.println("Error, cliente no detectado al revisar en el paquete.");
            } else{
                Jugador jugadorCliente = clientes[numeroCliente].getJugador();
                if (mensajePorPartes[0].equals("Aprete")) {
                    if (mensajePorPartes[1].equals("Izquierda")) {
                        jugadorCliente.moverIzquierda(true);
//                        enviarMensajeAtodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
//                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Derecha")) {

                        jugadorCliente.moverDerecha(true);
//                        enviarMensajeAtodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
//                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Arriba")) {

                        jugadorCliente.saltar();
//                        enviarMensajeAtodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
//                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    }
                } else if (mensajePorPartes[0].equals("NoAprete")) {
                    if (mensajePorPartes[1].equals("Izquierda")) {

                        jugadorCliente.moverIzquierda(false);
//                        enviarMensajeAtodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
//                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    } else if (mensajePorPartes[1].equals("Derecha")) {

                        jugadorCliente.moverDerecha(false);
//                        enviarMensajeAtodos("Movimiento-" + jugadorCliente.getPosicionX() + "-" +
//                            jugadorCliente.getPosicionY() + "-" + Jugadores.values()[numeroCliente]);

                    }
                }
                else if (mensajePorPartes[0].equals("SeSalioDelJuego")){
                    clientes[numeroCliente] = null;
                    int numeroJugador = Integer.parseInt(mensajePorPartes[1]);
                    cantClientes--;

                    System.out.println(
                        "Jugador " + (numeroCliente + 1) +
                            " desconectado. Jugadores conectados: " +
                            cantClientes
                    );

                    enviarMensajeAtodos("JugadorDesconectado");

                    fin = true;

                }

            }

            if (mensaje.equals("Hola")) {
                enviarMensaje("Hola", ipPaquete, puertoPaquete);
                clienteConfirmado = true;
            }


    }

    private int buscarEspacioLibre() {
        int espacio = -1;
        int i = 0;
        while (i < clientes.length && espacio == -1) {
            if (clientes[i] == null) {
                espacio = i;
            }
            i++;
        }

        return espacio;
    }

    private int detectarCliente(InetAddress ipPaquete, int puertoPaquete) {
        int numeroCliente = -1;
        for (int i = 0; i < clientes.length; i++) {
            if(clientes[i] != null) {
                DireccionRed direccionClienteActual = clientes[i].getDireccionRed();
                if (ipPaquete.equals(direccionClienteActual.getIp()) && puertoPaquete == direccionClienteActual.getPuerto()) {
                    numeroCliente = i;
                }
            }

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

    public void finalizarHilo(){
        fin = true;
        if (puertoServer != null && !puertoServer.isClosed()) {
            puertoServer.close();
        }
    }

    public boolean isFin() {
        return fin;
    }

    private void revisarTimeouts() {
        long ahora = System.currentTimeMillis();

        for (int i = 0; i < clientes.length; i++) {
            if (clientes[i] != null
                && !terminado[i]
                && ahora - ultimoContacto[i] > TIMEOUT_CLIENTE) {

                System.out.println("Jugador " + (i + 1) + " dejó de responder (cierre brusco o caída de red).");
                desconectarCliente(i);
            }
        }
    }

    private void desconectarCliente(int i) {
        if (clientes[i] == null) return;

        boolean avisarRival = !terminado[i];

        clientes[i] = null;
        cantClientes--;

        System.out.println("Jugadores conectados: " + cantClientes);

        if (avisarRival) {
            enviarMensajeAtodos("JugadorDesconectado");
            clienteDesconectado = true;
        }
    }

    public boolean isClienteDesconectado() {
        return clienteDesconectado;
    }

    public void setJugadorTerminado(int indice) {
        terminado[indice] = true;
    }

    //    public void restarCantidadClientes(){
//        this.cantClientes--;
//    }

//    public void borrarJugador(int numeroJugador){
//        this.clientes[numeroJugador] = null;
//    }

}
