package com.diva.diva;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class Correo {

    private static final String API_KEY = "re_4Cychs6R_PrcDmH8i4dfFwDnfSjEBdvpG";

    public static void enviarConfirmacionAsync(String correoDestino) {
        new Thread(() -> {
            Properties p = new Properties();
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.host", "smtp.resend.com");
            p.put("mail.smtp.port", "587");

            Session session = Session.getInstance(p, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("resend", API_KEY);
                }
            });

            try {
                Message m = new MimeMessage(session);
                m.setFrom(new InternetAddress("onboarding@resend.dev"));
                m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino));
                m.setSubject("No volverás a conectarte a ninguna red pública, verdad? :)");
                m.setText("Hola,\n\nTu correo ha sido verificado. Ya estás conectado a la red DIVA.");

                Transport.send(m);
                System.out.println("--> Correo de confirmación enviado exitosamente a: " + correoDestino);
            } catch (MessagingException e) {
                System.err.println("--> Error al enviar correo: " + e.getMessage());
            }
        }).start();
    }
}