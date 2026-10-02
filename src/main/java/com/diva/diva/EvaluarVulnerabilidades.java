/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.diva.diva;

/**
 *
 * @author carol
 */
public class EvaluarVulnerabilidades {
    
    
     // - Evalúa tres señales de exposición digital:
     // 1. Correo electrónico proporcionado voluntariamente.
     // 2. Información técnica del navegador.
    // 3. Uso de HTTP en lugar de HTTPS.
    
    public static List<ResultadoVulnerabilidad> evaluar(
            String correo,
            HttpExchange solicitud) {

        List<ResultadoVulnerabilidad> resultados = new ArrayList<>();

       
        // vulnerabilidades 1: CORREO ELECTRÓNICO
        

        if (correo == null || correo.isBlank()) {

            resultados.add(
                    new ResultadoVulnerabilidad(
                            "Correo electrónico",
                            "No proporcionado",
                            "Bajo",
                            0,
                            "No se proporcionó un correo electrónico."
                    )
            );

        } else {

            resultados.add(
                    new ResultadoVulnerabilidad(
                            "Correo electrónico",
                            "Correo proporcionado voluntariamente",
                            "Medio",
                            2,
                            "Evitar reutilizar contraseñas y activar autenticación de dos factores."
                    )
            );
        }

        
        // vulnerabilidades 2: INFORMACIÓN DEL NAVEGADOR
        

        String userAgent =
                solicitud.getRequestHeaders().getFirst("User-Agent");

        if (userAgent == null || userAgent.isBlank()) {

            resultados.add(
                    new ResultadoVulnerabilidad(
                            "Información del navegador",
                            "No identificada",
                            "Bajo",
                            0,
                            "No se obtuvo información del navegador."
                    )
            );

        } else {

            resultados.add(
                    new ResultadoVulnerabilidad(
                            "Huella del navegador",
                            "Información técnica visible",
                            "Medio",
                            2,
                            "Evitar introducir información sensible en redes públicas."
                    )
            );
        }

        
        // vulnerabilidades 3: HTTP
        

        resultados.add(
                new ResultadoVulnerabilidad(
                        "Cifrado de la conexión",
                        "HTTP detectado",
                        "Medio",
                        3,
                        "Utilizar conexiones HTTPS cuando estén disponibles."
                )
        );

        return resultados;
    }

    
    //Calcula el nivel de riesgo general.
    
    public static String calcularNivel(List<ResultadoVulnerabilidad> resultados) {

        int puntos = 0;

        for (ResultadoVulnerabilidad resultado : resultados) {
            puntos += resultado.getPuntos();
        }

        if (puntos <= 2) {
            return "BAJO";
        }

        if (puntos <= 5) {
            return "MEDIO";
        }

        if (puntos <= 8) {
            return "ALTO";
        }

        return "CRÍTICO";
    }

    
     //Calcula el total de puntos.
    
    public static int calcularPuntos(
            List<ResultadoVulnerabilidad> resultados) {

        int puntos = 0;

        for (ResultadoVulnerabilidad resultado : resultados) {
            puntos += resultado.getPuntos();
        }

        return puntos;
    }
}
}
