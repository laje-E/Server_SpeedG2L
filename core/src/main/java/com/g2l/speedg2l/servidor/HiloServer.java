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

    private boolean clienteConfirmado = false;

    private final int  TIEMPO_ESPERA_RECEIVE = 500;
    private final long TIMEOUT_CLIENTE       = 5000;

    private long[] ultimoContacto = new long[2];
    private boolean[] terminado   = new boolean[2];   // jugadores que ya llegaron a la meta


    private boolean clienteDesconectado = false;

    public HiloServer(){
        clientes = new Cliente[2];
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
        for (int i = 0; i < clientes.length; i++) {
            if (clientes[i] != null) {
                Jugador jugadorCliente = clientes[i].getJugador();
                enviarMensajeAtodos(
                    ProtocolosRed.MOVIMIENTO +
                    ProtocolosRed.SEPARADOR +
                    jugadorCliente.getPosicionX() +
                    ProtocolosRed.SEPARADOR +
                    jugadorCliente.getPosicionY() +
                    ProtocolosRed.SEPARADOR +
                    (i + 1)
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

        if (mensaje.equals(ProtocolosRed.ESTAS_AHI)) {
            if (numeroCliente != -1) {
                enviarMensaje(ProtocolosRed.ESTOY_ACA, ipPaquete, puertoPaquete);
            }
            return;
        }

        if (mensaje.equals(ProtocolosRed.CONEXION)) {
            if (numeroCliente != -1) {
                DireccionRed direccionCliente = clientes[numeroCliente].getDireccionRed();

                enviarMensaje(ProtocolosRed.OK, direccionCliente.getIp(), direccionCliente.getPuerto());

                if(cantClientes == 2) {
                    enviarMensaje(ProtocolosRed.EMPEZAR, direccionCliente.getIp(),direccionCliente.getPuerto());
                    enviarMensaje(
                        ProtocolosRed.NUMERO_JUGADOR +
                        ProtocolosRed.SEPARADOR +
                        (numeroCliente + 1),
                        direccionCliente.getIp(),
                        direccionCliente.getPuerto()
                    );
                }
                return;
            }

            if(cantClientes >= 2) {
                enviarMensaje(ProtocolosRed.ERROR_LIMITE_CLIENTES, ipPaquete, puertoPaquete);
                return;
            }

            int espacio = buscarEspacioLibre();

            if(espacio != -1) {
                DireccionRed direccionRed = new DireccionRed(ipPaquete, puertoPaquete);

                Jugador jugadorAsignado = (espacio == 0) ? jugador1 : jugador2;

                clientes[espacio] = new Cliente(direccionRed, jugadorAsignado);
                ultimoContacto[espacio] = System.currentTimeMillis();

                cantClientes++;

                enviarMensaje(ProtocolosRed.OK, ipPaquete, puertoPaquete);

                System.out.println("Cliente conectado: jugador " + (espacio + 1));

                if (cantClientes == 2) {

                    for (int i=0; i < cantClientes; i++) {
                        DireccionRed direccionRedCliente = clientes[i].getDireccionRed();

                        enviarMensaje(
                            ProtocolosRed.EMPEZAR,
                            direccionRedCliente.getIp(),
                            direccionRedCliente.getPuerto()
                        );

                        enviarMensaje(
                            ProtocolosRed.NUMERO_JUGADOR +
                            ProtocolosRed.SEPARADOR +
                            (i + 1),
                            direccionRedCliente.getIp(),
                            direccionRedCliente.getPuerto()
                        );
                    }

                    PantallaJuego.empezarJuego();
                }
            }

            return;
        }

        String[] mensajePorPartes = mensaje.split(ProtocolosRed.SEPARADOR);

        if(numeroCliente == -1){
                System.err.println("Error, cliente no detectado al revisar en el paquete.");
            }else{
                final int ACCION=0;
                final int SENTIDO=1;
                Jugador jugadorCliente = clientes[numeroCliente].getJugador();

                if(mensajePorPartes[ACCION].equals(ProtocolosRed.APRETE)) {

                    if(mensajePorPartes[SENTIDO].equals(ProtocolosRed.IZQUIERDA)) {
                        jugadorCliente.moverIzquierda(true);
                    }
                        else if (mensajePorPartes[SENTIDO].equals(ProtocolosRed.DERECHA)) {
                            jugadorCliente.moverDerecha(true);
                        }
                            else if (mensajePorPartes[SENTIDO].equals(ProtocolosRed.ARRIBA)) {
                                jugadorCliente.saltar();
                            }

                } else if (mensajePorPartes[ACCION].equals(ProtocolosRed.NO_APRETE)) {
                    if (mensajePorPartes[SENTIDO].equals(ProtocolosRed.IZQUIERDA)) {
                        jugadorCliente.moverIzquierda(false);
                    }
                        else if (mensajePorPartes[SENTIDO].equals(ProtocolosRed.DERECHA)) {
                            jugadorCliente.moverDerecha(false);
                        }
                }
                    else if(mensajePorPartes[ACCION].equals(ProtocolosRed.SE_SALIO_DEL_JUEGO)){
                        clientes[numeroCliente] = null;
                        cantClientes--;

                        System.out.println("Jugador " + (numeroCliente + 1) + " desconectado. Jugadores conectados: " + cantClientes);

                        enviarMensajeAtodos(ProtocolosRed.JUGADOR_DESCONECTADO);

                        fin=true;
                    }

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

        if(avisarRival){
            enviarMensajeAtodos(ProtocolosRed.JUGADOR_DESCONECTADO);
            clienteDesconectado = true;
        }
    }

    public boolean isClienteDesconectado() {
        return clienteDesconectado;
    }

    public boolean isClienteConfirmado() {
        return clienteConfirmado;
    }

    public void setJugadorTerminado(int indice) {
        terminado[indice] = true;
    }

}
