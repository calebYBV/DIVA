package com.diva.diva;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import com.resend.services.emails.model.CreateEmailResponse;

public class Correo {

    private final Resend servicio;
    private final String remitente;

    public Correo() {
        this(new Resend("re_3SUfSqgp_3YgV1LaqqdCk1uYotoFqJ8kV"), "onboarding@resend.dev");
    }

    Correo(Resend servicio, String remitente) {
        this.servicio = servicio;
        this.remitente = remitente;
    }

    public String enviar(String destino, String informe, String sesion) {
        if (servicio == null) {
            return "Correo no enviado: falta configurar el servicio de correo.";
        }
        String destinatarioFinal = (destino != null && !destino.isBlank()) ? destino : "calebsinm@gmail.com";
        CreateEmailOptions datos = CreateEmailOptions.builder().from(remitente).to(destinatarioFinal).subject("DIVA: resumen de tu prueba educativa").text(informe).html("<h1>DIVA</h1><pre>" + PaginaPortal.escapar(informe) + "</pre>").build();
        try {
            CreateEmailResponse respuesta = servicio.emails().send(datos);
            System.out.println("[CORREO] Solicitud aceptada por Resend. ID: " + respuesta.getId());
            return "Resend aceptó el envío. Revisa tu correo (" + destinatarioFinal + ") y la carpeta de spam.";
        } catch (ResendException e) {
            System.err.println("[CORREO] Error al enviar mediante Resend: " + e.getMessage());
            return "Resend rechazó o falló el envío. Revisa el correo destinatario e intenta descargar el informe.";
        } catch (RuntimeException e) {
            System.err.println("[CORREO] Error del cliente: " + e.getClass().getSimpleName());
            return "No se confirmó el envío. Puedes descargar el informe aquí.";
        }
    }
}
