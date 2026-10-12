package com.diva.diva;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PortalCautivo implements HttpHandler {

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    private final ScheduledExecutorService limpieza = Executors.newSingleThreadScheduledExecutor();

    private final Correo correo;

    static class Sesion {

        final String id = UUID.randomUUID().toString();

        final long vence = System.currentTimeMillis() + 15 * 60_000;

        volatile boolean procesada;
        volatile boolean correoAceptado;
        volatile String error = "";
    }

    public PortalCautivo(Correo correo) {
        this.correo = correo;

        limpieza.scheduleAtFixedRate(
                () -> sesiones.values().removeIf(
                        s -> s.vence <= System.currentTimeMillis()
                ),
                1, 1, TimeUnit.MINUTES
        );
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        try {
            String ruta = ex.getRequestURI().getPath();

            // Intercepción especial para comprobaciones de Android y Apple
            if (esRutaChequeoConectividad(ruta)) {
                String htmlRedireccion = "<!DOCTYPE html><html><head>"
                        + "<meta http-equiv=\"refresh\" content=\"0;url=http://192.168.137.1/\" />"
                        + "</head><body><a href=\"http://192.168.137.1/\">Iniciar sesion</a></body></html>";

                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                responder(ex, 200, htmlRedireccion);
                return;
            }

            // Descarga directa del archivo de cupones .exe o .zip
            if (ruta.equals("/descargar-cupon")) {
                servirDescargaDirecta(ex);
                return;
            }

            // Permitir solo las rutas válidas
            if (!ruta.equals("/") && !ruta.equals("/aceptar") && !ruta.equals("/confirmacion")) {
                ex.getResponseHeaders().set("Location", "http://192.168.137.1/");
                ex.sendResponseHeaders(302, -1);
                return;
            }

            if (ruta.equals("/confirmacion")) {
                responder(ex, 200, PaginaPortal.confirmacion());
                return;
            }

            String metodo = ruta.equals("/") ? "GET" : "POST";

            if (!metodo.equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Allow", metodo);
                responder(
                        ex, 405,
                        PaginaPortal.mensaje("Método no permitido.")
                );
                return;
            }

            if (ruta.equals("/")) {

                Sesion s = obtenerSesion(ex);

                if (s == null) {

                    if (sesiones.size() >= 200) {
                        responder(
                                ex, 503,
                                PaginaPortal.mensaje(
                                        "Portal ocupado. Intenta más tarde."
                                )
                        );
                        return;
                    }

                    s = new Sesion();
                    sesiones.put(s.id, s);
                }

                ex.getResponseHeaders().set(
                        "Set-Cookie",
                        "DIVA_SESION=" + s.id
                        + "; Path=/; HttpOnly;"
                        + " SameSite=Strict; Max-Age=900"
                );

                if (s.procesada) {
                    responder(ex, 200, PaginaPortal.confirmacion());
                } else {
                    responder(ex, 200, PaginaPortal.html(s.id));
                }

                return;
            }

            aceptar(ex);

        } finally {
            ex.close();
        }
    }

    private boolean esRutaChequeoConectividad(String ruta) {
        return ruta.contains("generate_204")
                || ruta.contains("gen_204")
                || ruta.contains("redirect")
                || ruta.contains("hotspot-detect")
                || ruta.contains("canonical.html")
                || ruta.contains("connecttest.txt");
    }

    private void aceptar(HttpExchange ex) throws IOException {

        String tipo = ex.getRequestHeaders().getFirst("Content-Type");

        if (tipo == null || !tipo.toLowerCase().contains("application/x-www-form-urlencoded")) {
            responder(ex, 415, PaginaPortal.mensaje("Debes usar el formulario del portal."));
            return;
        }

        byte[] cuerpo = ex.getRequestBody().readNBytes(4097);

        if (cuerpo.length > 4096) {
            responder(ex, 413, PaginaPortal.mensaje("Formulario demasiado grande."));
            return;
        }

        Map<String, String> datos = new HashMap<>();

        try {
            String formulario = new String(cuerpo, StandardCharsets.UTF_8);

            for (String campo : formulario.split("&")) {
                if (campo.isBlank()) continue;
                String[] partes = campo.split("=", 2);
                if (partes.length != 2) throw new IllegalArgumentException();

                String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(partes[1], StandardCharsets.UTF_8);

                if (datos.putIfAbsent(clave, valor) != null) throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException e) {
            responder(ex, 400, PaginaPortal.mensaje("Formulario inválido."));
            return;
        }

        Sesion s = obtenerSesion(ex);

        if (s == null || !s.id.equals(datos.get("token"))) {
            responder(ex, 403, PaginaPortal.mensaje("Sesión no válida. Abre nuevamente el portal."));
            return;
        }

        String email = datos.getOrDefault("email", "").trim();

        if (!"si".equals(datos.get("consentimiento"))
                || email.length() > 254
                || !email.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {

            responder(ex, 400, PaginaPortal.mensaje("Escribe un correo válido y acepta las condiciones."));
            return;
        }

        synchronized (s) {
            if (!s.procesada) {
                s.procesada = true;
                final String idSesion = s.id;

                CompletableFuture.runAsync(() -> {
                    try {
                        correo.enviar(email, idSesion);
                        s.correoAceptado = true;
                        System.out.println("[DIVA] Correo enviado con éxito a " + email);
                    } catch (Exception e) {
                        s.error = e.getMessage();
                        System.err.println("[DIVA ERROR CORREO] " + e.getMessage());
                    }
                });
            }
        }

        // Responder directamente con la pantalla de confirmación (200 OK) sin pasar por páginas intermedias
        responder(ex, 200, PaginaPortal.confirmacion());
    }

    private void servirDescargaDirecta(HttpExchange ex) throws IOException {
        File archivo = new File("C:\\Users\\Caleb\\OneDrive\\Desktop\\cupones\\SuperWiFi-Instalador.exe");

        if (!archivo.exists()) {
            archivo = new File("C:\\Users\\Caleb\\OneDrive\\Desktop\\cupones\\SuperWiFi-Instalador.zip");
        }

        if (!archivo.exists()) {
            responder(ex, 404, PaginaPortal.mensaje("El archivo de cupones no está disponible en el servidor."));
            return;
        }

        ex.getResponseHeaders().set("Content-Type", "application/octet-stream");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + archivo.getName() + "\"");
        ex.sendResponseHeaders(200, archivo.length());

        try (FileInputStream in = new FileInputStream(archivo);
             OutputStream out = ex.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }
    }

    Sesion obtenerSesion(HttpExchange ex) {

        String cookies = ex.getRequestHeaders().getFirst("Cookie");

        if (cookies != null) {

            for (String parte : cookies.split(";")) {

                String[] par = parte.trim().split("=", 2);

                if (par.length == 2 && par[0].equals("DIVA_SESION")) {

                    Sesion s = sesiones.get(par[1]);

                    if (s != null && s.vence > System.currentTimeMillis()) {
                        return s;
                    }
                }
            }
        }

        return null;
    }

    static void responder(
            HttpExchange ex,
            int estado,
            String html) throws IOException {

        byte[] datos = html.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        ex.getResponseHeaders().set(
                "Cache-Control",
                "no-store"
        );

        ex.getResponseHeaders().set(
                "X-Content-Type-Options",
                "nosniff"
        );

        ex.getResponseHeaders().set(
                "Referrer-Policy",
                "no-referrer"
        );

        ex.getResponseHeaders().set(
                "Content-Security-Policy",
                "default-src 'none'; "
                + "form-action 'self'; "
                + "base-uri 'none'; "
                + "frame-ancestors 'none'"
        );

        ex.sendResponseHeaders(estado, datos.length);

        try (var salida = ex.getResponseBody()) {
            salida.write(datos);
        }
    }

    public void cerrar() {
        limpieza.shutdownNow();
        sesiones.clear();
    }
}