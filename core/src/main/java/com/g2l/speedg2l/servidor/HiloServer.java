package com.g2l.speedg2l.servidor;

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

    HiloServer(){
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

    @Override
    public void run() {
        do{
            byte [] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);
            try{
                puertoServer.receive(dp);
            }catch (IOException event){
                event.printStackTrace();
            }
            procesarMensaje(dp);
        }while(!fin);
    }

    private void procesarMensaje(DatagramPacket dp){
        String mensaje = dp.getData().toString().trim();
        if(mensaje.equals("Conexion")){
            if(cantClientes == 0){
                direccionesClientes[0] = new DireccionRed(dp.getAddress().toString(), dp.getPort());
                cantClientes++;
            }
            else if(cantClientes == 1){
                direccionesClientes[1] = new DireccionRed(dp.getAddress().toString(), dp.getPort());
                cantClientes++;
            }
            else{
                enviarMensaje("ERROR-limiteDeClientesAlcanzado", dp.getAddress(), dp.getPort());
            }
        }
    }

}
