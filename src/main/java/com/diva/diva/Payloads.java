package com.diva.diva;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Payloads {
    // Directorio configurable. Por defecto apunta a ~/Desktop/payloads
    private static final String PAYLOADS_DIR = System.getProperty("diva.payloads.dir", 
            System.getProperty("user.home") + "/Desktop/payloads/");

    public record PayloadData(byte[] content, String filename, String mimeType) {}

    /**
     * Mantiene compatibilidad con PortalCautivo para la generación del informe por correo.
     */
    public static String crearInforme(HttpExchange solicitud) {
        return """
                ¡Hola!
                
                Gracias por registrarte en el portal de DIVA.
                
                Este es un correo de prueba enviado automáticamente. Si recibes este mensaje, la prueba se ha completado correctamente.
                
                ¡Que tengas un excelente día!
                """;
    }

    /**
     * Detecta SO desde User-Agent y devuelve el payload correspondiente.
     * Si el archivo no existe, retorna un fallback genérico.
     */
    public static PayloadData obtenerPayloadPorOS(String userAgent) {
        String os = "generic";
        if (userAgent != null) {
            String ua = userAgent.toLowerCase();
            if (ua.contains("windows")) os = "windows";
            else if (ua.contains("mac os") || ua.contains("macintosh")) os = "macos";
            else if (ua.contains("linux")) os = "linux";
            else if (ua.contains("android")) os = "android";
            else if (ua.contains("iphone") || ua.contains("ipad")) os = "ios";
        }

        String filename = switch (os) {
            case "windows" -> "demo_win.exe";
            case "macos"   -> "demo_mac.app";
            case "linux"   -> "demo_linux.sh";
            case "android" -> "demo_android.apk";
            case "ios"     -> "demo_ios.mobileconfig";
            default        -> "demo_generic.txt";
        };

        Path ruta = Paths.get(PAYLOADS_DIR, filename);
        String mimeType = switch (os) {
            case "windows" -> "application/x-msdownload";
            case "macos"   -> "application/x-macbinary";
            case "linux"   -> "application/x-sh";
            case "android" -> "application/vnd.android.package-archive";
            case "ios"     -> "application/x-apple-aspen-config";
            default        -> "application/octet-stream";
        };

        try {
            if (Files.exists(ruta) && Files.isReadable(ruta)) {
                byte[] data = Files.readAllBytes(ruta);
                return new PayloadData(data, filename, mimeType);
            }
        } catch (IOException e) {
            System.err.println("[PAYLOADS] Error leyendo " + ruta + ": " + e.getMessage());
        }

        // Fallback seguro si el archivo no existe o falla la lectura
        String fallback = "DIVA: Archivo de demostración no encontrado en " + PAYLOADS_DIR;
        return new PayloadData(fallback.getBytes(), "demo_fallback.txt", "text/plain");
    }
}