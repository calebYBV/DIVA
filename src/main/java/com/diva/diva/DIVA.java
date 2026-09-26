package com.diva.diva;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DIVA {

    public static void main(String[] args) throws Exception {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("0.0.0.0", 8080), 0
        );

        servidor.createContext("/", solicitud -> {

            String texto = "DIVA está funcionando correctamente";

            byte[] respuesta =
                    texto.getBytes(StandardCharsets.UTF_8);

            solicitud.getResponseHeaders().set(
                    "Content-Type",
                    "text/plain; charset=UTF-8"
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
    }
}