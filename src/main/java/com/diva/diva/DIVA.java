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

            String pagina = """
                    <html>
                    <body>
hola
                    <h1>DIVA</h1>

                    <p>
                    DIVA realizará una prueba educativa de seguridad.
                    No accederá ni modificará tu información personal.
                    </p>

                    <form action="/aceptar" method="POST">

                        <p>Correo electrónico:</p>

                        <input type="email" name="correo" required>

                        <br><br>

                        <input type="checkbox"
                               name="terminos"
                               required>

                        Acepto participar.

                        <br><br>

                        <button type="submit">
                            Participar
                        </button>

                    </form>

                    </body>
                    </html>
                    """;

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

            String mensaje = """
                    <html>
                    <body>

                    <h1>Consentimiento registrado</h1>

                    <p>
                    Gracias por participar en DIVA.
                    </p>

                    </body>
                    </html>
                    """;

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
    }
}