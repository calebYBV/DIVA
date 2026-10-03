package com.diva.diva;

public class paginas {

    public static String paginaPrincipal() {

        return """
                <html>
                <body>

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
    }

    public static String paginaConfirmacion() {

        return """
                <html>
                <body>

                <h1>Consentimiento registrado</h1>

                <p>
                Gracias por participar en DIVA.
                </p>

                </body>
                </html>
                """;
    }
}