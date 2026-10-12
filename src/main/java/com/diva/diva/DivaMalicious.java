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
            if (!"/confirmacion".equals(
                    ex.getRequestURI().getPath())) {

                PortalCautivo.responder(
                        ex, 404,
                        PaginaPortal.mensaje("Página no encontrada.")
                );
                return;
            }

            if (!"GET".equals(ex.getRequestMethod())) {

                ex.getResponseHeaders().set("Allow", "GET");

                PortalCautivo.responder(
                        ex, 405,
                        PaginaPortal.mensaje("Método no permitido.")
                );
                return;
            }

            PortalCautivo.Sesion s = portal.obtenerSesion(ex);

            if (s == null || !s.procesada) {

                PortalCautivo.responder(
                        ex, 403,
                        PaginaPortal.mensaje(
                                "Primero completa el formulario."
                        )
                );
                return;
            }

            if (s.correoAceptado) {

                PortalCautivo.responder(
                        ex, 200,
                        PaginaPortal.confirmacion()
                );

            } else {

                PortalCautivo.responder(
                        ex, 502,
                        PaginaPortal.mensaje(s.error)
                );
            }

        } finally {
            ex.close();
        }
    }
}