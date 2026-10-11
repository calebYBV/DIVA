package com.diva.diva;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HotspotNetsh {
    public static void activar() throws Exception {
        System.out.println("[NETSH] Configurando Hotspot...");
        
        ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "set", "hostednetwork", "mode=allow", "ssid=DIVA-Net", "key=12345678");
        pb.redirectErrorStream(true);
        Process p1 = pb.start();
        readProcess(p1);

        System.out.println("[NETSH] Iniciando red...");
        pb = new ProcessBuilder("netsh", "wlan", "start", "hostednetwork");
        Process p2 = pb.start();
        readProcess(p2);
        
        System.out.println("[NETSH] Hotspot activado exitosamente.");
    }

    private static void readProcess(Process p) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println("[NETSH] " + line);
            }
        }
        if (p.waitFor() != 0) throw new RuntimeException("Error en comando netsh");
    }
}