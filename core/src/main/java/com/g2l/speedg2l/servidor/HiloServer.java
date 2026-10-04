package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.pantallas.PantallaJuego;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HiloServer extends Thread{

    private InetAddress direccionServer;
    private DatagramSocket puertoServer;
    private boolean fin = false;
    private DireccionRed [] direccionesClientes = new DireccionRed[2];
    private int cantClientes = 0;

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
            System.out.println("Mensaje: " + (new String (dp.getData())).trim());
            puertoServer.send(dp);
        }catch (IOException event){
            event.printStackTrace();
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

                System.out.println("Recibí un paquete de: "
                    + dp.getAddress()
                    + ":"
                    + dp.getPort());

            } catch (IOException event) {
                event.printStackTrace();
            }

            procesarMensaje(dp);

        } while(!fin);
    }

    private void procesarMensaje(DatagramPacket dp){
        String mensaje = (new String (dp.getData())).trim();
        System.out.println("mensaje cliente: " + mensaje);
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
                if (cantClientes == 2){
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
    }

    public int getCantClientes() {
        return cantClientes;
    }
}
