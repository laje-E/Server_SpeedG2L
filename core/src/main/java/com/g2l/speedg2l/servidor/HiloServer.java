package com.g2l.speedg2l.servidor;

import com.g2l.speedg2l.entidades.Jugador;
import com.g2l.speedg2l.pantallas.PantallaJuego;

import java.io.IOException;
import java.net.*;

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

    private final int SEGUNDOS_CUENTA = 5;
    private int cuentaRegresiva = 0;
    private boolean cuentaEnCurso = false;
    private boolean partidaIniciada = false;
    private long inicioCuenta = 0;
    private int ultimoNumeroEnviado = -1;

    private boolean[] quiereRevancha = new boolean[2];

    private boolean partidaFinalizada = false;
    private boolean reinicioSolicitado = false;
    private boolean finSesionSolicitado = false;

    public HiloServer(){
        clientes = new Cliente[2];
        try {
            direccionServer = InetAddress.getByName("255.255.255.255");
            puertoServer = new DatagramSocket(6412);
            puertoServer.setSoTimeout(TIEMPO_ESPERA_RECEIVE);
        }catch (SocketException | UnknownHostException evento){
            evento.printStackTrace();
            fin = true;
            if (puertoServer != null && !puertoServer.isClosed()) {
                puertoServer.close();
            }
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

            byte[] data = new byte[1024];
            DatagramPacket dp = new DatagramPacket(data, data.length);

            try{
                puertoServer.receive(dp);
                procesarMensaje(dp);

            }catch(SocketTimeoutException timeout){
                // Aca se agoto el tiempo de espera para recibir un paquete.
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

    private void evaluarMensaje(String mensaje, InetAddress ipPaquete, int puertoPaquete) {

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
            procesarConexion(ipPaquete, puertoPaquete, numeroCliente);
            return;
        }

        if (numeroCliente != -1) {

            if (mensaje.equals(ProtocolosRed.JUGAR_OTRA)) {

                quiereRevancha[numeroCliente] = true;

                enviarMensaje(
                    ProtocolosRed.ESPERANDO_REVANCHA,
                    ipPaquete,
                    puertoPaquete
                );

                if (quiereRevancha[0] && quiereRevancha[1]) {

                    reinicioSolicitado = true;
                    partidaFinalizada = false;

                    enviarMensajeAtodos(ProtocolosRed.NUEVA_PARTIDA);
                }

                return;
            }

            if (mensaje.equals(ProtocolosRed.VOLVER_MENU)) {

                finSesionSolicitado = true;
                enviarMensajeAtodos(ProtocolosRed.FIN_SESION);

                return;
            }
        }

        if (numeroCliente == -1) {
            System.err.println("Error, cliente no detectado al revisar en el paquete.");
            return;
        }

        String[] mensajePorPartes = mensaje.split(ProtocolosRed.SEPARADOR);

        procesarAccionJugador(mensajePorPartes, numeroCliente);
    }

    private void procesarConexion(InetAddress ipPaquete, int puertoPaquete, int numeroCliente) {

        if (numeroCliente != -1) {
            DireccionRed direccionCliente = clientes[numeroCliente].getDireccionRed();

            enviarMensaje(ProtocolosRed.OK, direccionCliente.getIp(), direccionCliente.getPuerto());

            if (cantClientes == 2) {
                enviarMensaje(
                    ProtocolosRed.NUMERO_JUGADOR +
                        ProtocolosRed.SEPARADOR +
                        (numeroCliente + 1),
                    direccionCliente.getIp(),
                    direccionCliente.getPuerto()
                );

                if (partidaIniciada) {
                    enviarMensajeAtodos(ProtocolosRed.EMPEZAR);
                }
            }

            return;
        }

        if (cantClientes >= 2) {
            enviarMensaje(ProtocolosRed.ERROR_LIMITE_CLIENTES, ipPaquete, puertoPaquete);
            return;
        }

        int espacio = buscarEspacioLibre();

        if (espacio != -1) {
            DireccionRed direccionRed = new DireccionRed(ipPaquete, puertoPaquete);

            Jugador jugadorAsignado = (espacio == 0) ? jugador1 : jugador2;

            clientes[espacio] = new Cliente(direccionRed, jugadorAsignado);
            ultimoContacto[espacio] = System.currentTimeMillis();

            cantClientes++;

            enviarMensaje(ProtocolosRed.OK, ipPaquete, puertoPaquete);

            System.out.println("Cliente conectado: jugador " + (espacio + 1));

            if (cantClientes == 2) {

                for (int i = 0; i < cantClientes; i++) {
                    DireccionRed direccionRedCliente = clientes[i].getDireccionRed();

                    enviarMensaje(
                        ProtocolosRed.NUMERO_JUGADOR +
                            ProtocolosRed.SEPARADOR +
                            (i + 1),
                        direccionRedCliente.getIp(),
                        direccionRedCliente.getPuerto()
                    );
                }

                iniciarCuentaRegresiva();
            }
        }
    }

    private void procesarAccionJugador(String[] mensajePorPartes, int numeroCliente) {
        final int ACCION = 0;
        final int SENTIDO = 1;

        Jugador jugadorCliente = clientes[numeroCliente].getJugador();

        if (mensajePorPartes[ACCION].equals(ProtocolosRed.APRETE)) {

            if (mensajePorPartes[SENTIDO].equals(ProtocolosRed.IZQUIERDA)) {
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

        } else if (mensajePorPartes[ACCION].equals(ProtocolosRed.SE_SALIO_DEL_JUEGO)) {
            desconectarCliente(numeroCliente);
        }
    }

    public void reiniciarPartida() {
        terminado = new boolean[2];
        quiereRevancha = new boolean[2];

        long ahora = System.currentTimeMillis();
        for (int i = 0; i < ultimoContacto.length; i++) {
            ultimoContacto[i] = ahora;
        }

        partidaIniciada = false;
        cuentaEnCurso = false;
        cuentaRegresiva = 0;
        ultimoNumeroEnviado = -1;
        finSesionSolicitado = false;
        partidaFinalizada = false;

        iniciarCuentaRegresiva();
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

        Jugador jugadorDesconectado = clientes[i].getJugador();

        jugadorDesconectado.moverIzquierda(false);
        jugadorDesconectado.moverDerecha(false);

        clientes[i] = null;
        cantClientes--;

        System.out.println("Jugador " + (i + 1) + " desconectado. Jugadores conectados: " + cantClientes);

        if (cuentaEnCurso) {
            cancelarCuenta();
        }

        enviarMensajeAtodos(ProtocolosRed.JUGADOR_DESCONECTADO);

        clienteDesconectado = true;

        finalizarHilo();
    }

    private void iniciarCuentaRegresiva() {
        if (!cuentaEnCurso && !partidaIniciada) {
            ultimoNumeroEnviado = -1;
            inicioCuenta = System.currentTimeMillis();
            cuentaEnCurso = true;
        }
    }

    public void actualizarCuentaRegresiva() {
        if (cuentaEnCurso) {
            long transcurrido = System.currentTimeMillis() - inicioCuenta;
            int restante = SEGUNDOS_CUENTA - (int) (transcurrido / 1000);

            if (restante <= 0) {
                cuentaRegresiva = 0;
                cuentaEnCurso = false;
                partidaIniciada = true;

                enviarMensajeAtodos(ProtocolosRed.EMPEZAR);
                PantallaJuego.empezarJuego();
                return;
            }

            // Solo mandamos el mensaje cuando cambia el número (una vez por segundo)
            if (restante != ultimoNumeroEnviado) {
                ultimoNumeroEnviado = restante;
                cuentaRegresiva = restante;
                enviarMensajeAtodos(ProtocolosRed.CUENTA_REGRESIVA + ProtocolosRed.SEPARADOR + restante);
            }
        }
    }

    private void cancelarCuenta() {
        cuentaRegresiva = 0;
        cuentaEnCurso = false;
        enviarMensajeAtodos(ProtocolosRed.CUENTA_REGRESIVA + ProtocolosRed.SEPARADOR + 0);
    }

    public int getCuentaRegresiva() {
        return cuentaRegresiva;
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


    public void marcarPartidaFinalizada() {
        partidaFinalizada = true;
    }

    public boolean isReinicioSolicitado() {
        return reinicioSolicitado;
    }

    public boolean isFinSesionSolicitado() {
        return finSesionSolicitado;
    }

    public void confirmarReinicio() {
        reinicioSolicitado = false;
    }

}
