package com.diva.diva;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        volatile String informe;
        volatile String estadoCorreo = "Envío en proceso.";
    }

    public PortalCautivo(Correo correo) {
        this.correo = correo;
        limpieza.scheduleAtFixedRate(() -> sesiones.values().removeIf(s -> s.vence <= System.currentTimeMillis()), 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            if (ruta.equals("/")) {
                if (!"GET".equals(ex.getRequestMethod())) {
                    responder(ex, 405, PaginaPortal.mensaje("Método no permitido."));
                    return;
                }
                Sesion s = obtenerSesion(ex);
                if (s == null) {
                    if (sesiones.size() >= 200) {
                        responder(ex, 503, PaginaPortal.mensaje("Portal ocupado. Intenta más tarde."));
                        return;
                    }
                    s = new Sesion();
                    sesiones.put(s.id, s);
                }
                ex.getResponseHeaders().set("Set-Cookie", "DIVA_SESION=" + s.id + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=900");
                if (s.informe != null) {
                    redirigir(ex);
                } else {
                    responder(ex, 200, PaginaPortal.html(s.id));
                }
                return;
            }
            if (ruta.equals("/aceptar")) {
                if (!"POST".equals(ex.getRequestMethod())) {
                    responder(ex, 405, PaginaPortal.mensaje("Método no permitido."));
                    return;
                }
                aceptar(ex);
                return;
            }
            ex.getResponseHeaders().set("Location", "/");
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.sendResponseHeaders(302, -1);
        } finally {
            ex.close();
        }
    }

    private void aceptar(HttpExchange ex) throws IOException {
        String tipo = ex.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
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
            for (String campo : new String(cuerpo, StandardCharsets.UTF_8).split("&")) {
                String[] partes = campo.split("=", 2);
                if (partes.length != 2) {
                    throw new IllegalArgumentException();
                }
                String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
                if (datos.putIfAbsent(clave, valor) != null) {
                    throw new IllegalArgumentException();
                }
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
        if (!"si".equals(datos.get("consentimiento")) || email.length() > 254 || !email.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            responder(ex, 400, PaginaPortal.mensaje("Escribe un correo válido y acepta las condiciones."));
            return;
        }
        synchronized (s) {
            if (s.informe == null) {
                s.informe = Payloads.crearInforme(ex);
                s.estadoCorreo = correo.enviar(email, s.informe, s.id);
                System.out.println("[DIVA] Participación procesada.");
            }
        }
        redirigir(ex);
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

    private static void redirigir(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Location", "/malicious");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(303, -1);
    }

    static void responder(HttpExchange ex, int estado, String html) throws IOException {
        byte[] datos = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; " + "style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
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
