package com.diva.diva; import com.sun.jna.Platform; import java.net.DatagramPacket; import java.net.DatagramSocket;

import java.net.InetAddress; public class HostPot {

    private static volatile boolean dnsActivo = false;
    private static DatagramSocket socketDNS;

    public static void activar() throws Exception {
        if (!Platform.isWindows()) {
            throw new IllegalStateException("El hotspot nativo requiere Windows 10 u 11.");
        }
        String comando = "powershell -Command \"[Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]; " + "$profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile(); " + "$manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($profile); " + "$async = $manager.StartTetheringAsync(); " + "while ($async.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }; " + "$result = $async.GetResults(); " + "if ($result.Status -ne 'Success') { exit 1 }\"";
        Process proceso = Runtime.getRuntime().exec(comando);
        int codigoSalida = proceso.waitFor();
        if (codigoSalida != 0) {
            System.err.println("[WIFI] No se pudo encender el hotspot automáticamente.");
        }
    }

    public static void desactivar() {
        dnsActivo = false;
        if (socketDNS != null && !socketDNS.isClosed()) {
            socketDNS.close();
        }
        if (!Platform.isWindows()) {
            return;
        }
        try {
            String comando = "powershell -Command \"[Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]; " + "$profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile(); " + "$manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($profile); " + "$manager.StopTetheringAsync()\"";
            Runtime.getRuntime().exec(comando);
            System.out.println("[WIFI] Hotspot desactivado.");
        } catch (Exception e) {
            System.err.println("[WIFI] Error al desactivar hotspot: " + e.getMessage());
        }
    }

    public static void iniciarDNS(String ipRespuesta) {
        dnsActivo = true;
        Thread threadDNS = new Thread(() -> {
            try {
                socketDNS = new DatagramSocket(53);
                byte[] buffer = new byte[512];
                byte[] ipBytes = InetAddress.getByName(ipRespuesta).getAddress();
                while (dnsActivo) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socketDNS.receive(paquete);
                    byte[] data = paquete.getData();
                    int len = paquete.getLength();
                    if (len < 12) {
                        continue;
                    }
                    byte[] respuesta = new byte[len + 16];
                    System.arraycopy(data, 0, respuesta, 0, len);
                    respuesta[2] = (byte) 0x81;
                    respuesta[3] = (byte) 0x80;
                    respuesta[6] = 0x00;
                    respuesta[7] = 0x01;
                    int pos = len;
                    respuesta[pos++] = (byte) 0xC0;
                    respuesta[pos++] = 0x0C;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = 0x01;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = 0x01;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = (byte) 0x3C;
                    respuesta[pos++] = 0x00;
                    respuesta[pos++] = 0x04;
                    System.arraycopy(ipBytes, 0, respuesta, pos, 4);
                    DatagramPacket paqueteRespuesta = new DatagramPacket(respuesta, pos + 4, paquete.getAddress(), paquete.getPort());
                    socketDNS.send(paqueteRespuesta);
                }
            } catch (Exception e) {
                if (dnsActivo) {
                    System.err.println("[DNS] Requiere permisos de Administrador para abrir el puerto UDP 53.");
                }
            }
        });
        threadDNS.setDaemon(true);
        threadDNS.start();
    }
}