package com.diva.diva;

import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DIVA {

    public static void main(String[] args) throws Exception {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("0.0.0.0", 8080), 0
        );

        servidor.createContext("/", solicitud -> {

            // Obtenemos el contenido desde la clase paginas.
            String pagina = paginas.paginaPrincipal();

            byte[] respuesta =
                    pagina.getBytes(StandardCharsets.UTF_8);

            solicitud.getResponseHeaders().set(
                    "Content-Type",
                    "text/html; charset=UTF-8"
            );

            solicitud.sendResponseHeaders(
                    200,
                    respuesta.length
            );

            solicitud.getResponseBody().write(respuesta);
            solicitud.getResponseBody().close();
        });

        servidor.createContext("/aceptar", solicitud -> {

            String datos = new String(
                    solicitud.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            System.out.println(
                    "Consentimiento recibido: " + datos
            );

            // Obtenemos la confirmación desde la clase paginas.
            String mensaje = paginas.paginaConfirmacion();

            byte[] respuesta =
                    mensaje.getBytes(StandardCharsets.UTF_8);

            solicitud.getResponseHeaders().set(
                    "Content-Type",
                    "text/html; charset=UTF-8"
            );

            solicitud.sendResponseHeaders(
                    200,
                    respuesta.length
            );

            solicitud.getResponseBody().write(respuesta);
            solicitud.getResponseBody().close();
        });

        servidor.start();

        System.out.println(
                "Servidor DIVA encendido en http://localhost:8080"
        );

        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(
                        Desktop.Action.BROWSE)) {

            Desktop.getDesktop().browse(
                    new URI("http://localhost:8080")
            );
        }
    }
}