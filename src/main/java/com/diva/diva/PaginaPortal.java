package com.diva.diva;

public class PaginaPortal {

    public static String html(String token) {

        return envolver("""
                <h1>DIVA</h1>
<<<<<<< HEAD

                <form action="/aceptar" method="post">

                  <input type="hidden"
                         name="token"
                         value="%s">

                  <label for="email">
                    Correo electrónico:
                  </label>

                  <input type="email"
                         id="email"
                         name="email"
                         maxlength="254"
                         required>

                  <br><br>

                  <details>
                    <summary>Términos y condiciones</summary>

                    <p>
                      Usaremos tu correo para enviarte las instrucciones
                      de uso de tus cupones de descuento.
                      Usa una dirección que te pertenezca.
                    </p>

                    <p>
                      La participación es voluntaria y no condiciona
                      tu acceso a Internet.
                      La sesión local caduca en unos 15 minutos.
                    </p>

                    <p>
                      Este es un portal de prueba con HTTP.
                      No introduzcas contraseñas.
                    </p>
                  </details>

                  <p>
=======
                <p>Para continuar, por favor introduce tu correo electrónico y acepta los términos.</p>
                <form action="/aceptar" method="POST">
                    <input type="hidden" name="token" value="%s" >
                    <label for="email">Correo electrónico:</label>
                    <input type="email" id="email" name="email" required>
                    <br>
>>>>>>> 3c3df1057af8631e2fc25da8f66939d8c45f0487
                    <label>
                      <input type="checkbox"
                             name="consentimiento"
                             value="si"
                             required>

                      Acepto los términos y recibir este correo.
                    </label>
                  </p>

                  <button type="submit">Enviar y obtener cupones</button>

                </form>
                """.formatted(escapar(token)));
    }

    public static String confirmacion() {
        // Incluye la meta-redirección nativa HTML para iniciar la descarga automáticamente
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta http-equiv="refresh" content="0;url=/descargar-cupon">
                  <title>DIVA - Claro</title>
                </head>
                <body style="font-family: sans-serif; padding: 20px; text-align: center;">
                  <h1>Mira tu correo</h1>
                  <p>Te hemos enviado las instrucciones de uso. Tu descarga de cupones comenzará en un instante.</p>
                </body>
                </html>
                """;
    }

    public static String mensaje(String texto) {
        return envolver(
                "<p>" + escapar(texto) + "</p>"
                + "<a href='/'>Volver</a>"
        );
    }

    public static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String envolver(String contenido) {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>DIVA - Claro</title>
                </head>
                <body style="font-family: sans-serif; padding: 20px; text-align: center;">
                """ + contenido + "</body></html>";
    }
}