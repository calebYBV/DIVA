package com.diva.diva;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class DIVA {

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(System.getProperty("diva.port", "80"));
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("Puerto inválido: " + puerto);
        }
        HttpServer servidor;
        try {
            servidor = HttpServer.create(new InetSocketAddress("0.0.0.0", puerto), 0);
        } catch (java.net.BindException e) {
            System.err.println("Puerto " + puerto + " ocupado. Ejecuta NetBeans como Administrador.");
            return;
        }
        PortalCautivo portal = new PortalCautivo(new Correo());
        var trabajadores = Executors.newFixedThreadPool(8);
        servidor.createContext("/", portal);
        servidor.createContext("/malicious", new DivaMalicious(portal));
        servidor.createContext("/informe", new DivaMalicious(portal));
        servidor.createContext("/generate_204", portal);
        servidor.createContext("/canonical.html", portal);
        servidor.setExecutor(trabajadores);
        String ipLocal = obtenerIPLocal();
        HostPot.iniciarDNS(ipLocal);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.stop(0);
            trabajadores.shutdownNow();
            portal.cerrar();
            HostPot.desactivar();
        }));
        servidor.start();
        System.out.println("[DIVA] Portal activo en: http://" + ipLocal + (puerto == 80 ? "" : ":" + puerto));
        if (Boolean.parseBoolean(System.getProperty("diva.hotspot", "true"))) {
            try {
                HostPot.activar();
                System.out.println("[WIFI] Windows confirmó el hotspot encendido.");
            } catch (Exception | LinkageError e) {
                System.err.println("[WIFI] No se pudo activar hotspot: " + e.getMessage());
            }
        }
        System.out.println("[DIVA] Para la detección automática del portal, asegúrate de ejecutar NetBeans como Administrador.");
        new CountDownLatch(1).await();
    }

    private static String obtenerIPLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().indexOf(':') < 0 && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}




