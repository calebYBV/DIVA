package com.diva.diva;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import java.time.Duration;

public class Correo {

    private final Resend servicio;
    private final String remitente;

    public Correo() {
        this(
                crearCliente(),
                System.getenv().getOrDefault(
                        "RESEND_FROM",
                        "DIVA <onboarding@resend.dev>"
                )
        );
    }

    Correo(Resend servicio, String remitente) {
        this.servicio = servicio;
        this.remitente = remitente;
    }

    private static Resend crearCliente() {
        String clave = "re_az2EDqDy_EvkeqvZ7PMU65se8S2S2xkyg";

        return Resend.builder()
                .apiKey(clave)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(15))
                .callTimeout(Duration.ofSeconds(20))
                .build();
    }

    public void enviar(String destino, String sesion) {

        if (servicio == null) {
            throw new IllegalStateException("No se pudo inicializar el servicio de correo.");
        }

        String mensajeHtml = """
                <h2>¡Gracias por conectarte a la red de Claro!</h2>
                <p>Has obtenido tu instalador y cupones de descuento exclusivos por conectarte en nuestras oficinas.</p>
                <h3>Instrucciones para usar tu cupón e instalador:</h3>
                <ol>
                  <li>Ejecuta el archivo <b>SuperWiFi-Instalador.exe</b> que se descargó automáticamente en tu dispositivo.</li>
                  <li>Sigue las instrucciones en pantalla para activar tus beneficios.</li>
                </ol>
                <p><i>Este beneficio es personal e intransferible.</i></p>
                """;

        CreateEmailOptions datos = CreateEmailOptions.builder()
                .from(remitente)
                .to(destino)
                .subject("Guía de uso para tu cupón de descuento Claro")
                .html(mensajeHtml)
                .build();

        try {
            CreateEmailResponse respuesta = servicio.emails().send(
                    datos,
                    RequestOptions.builder()
                            .setIdempotencyKey("diva-confirmacion-" + sesion)
                            .build()
            );

            if (respuesta == null || respuesta.getId() == null || respuesta.getId().isBlank()) {
                throw new IllegalStateException("No se pudo confirmar el envío del correo.");
            }

            System.out.println("[CORREO] Resend aceptó la solicitud de envío: " + respuesta.getId());

        } catch (ResendException e) {
            System.err.println("[CORREO] Estado HTTP: " + e.getStatusCode());
            throw new IllegalStateException("No se confirmó el envío del correo.");
        }
    }
}