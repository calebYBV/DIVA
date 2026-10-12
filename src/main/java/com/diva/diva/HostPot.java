package com.diva.diva;


import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

public class HostPot {

    private static class Nativa {
        static final WinAPI API = Native.load("combase", WinAPI.class);
    }

    public static void activar() throws InterruptedException {

        if (!Platform.isWindows()) {
            throw new IllegalStateException(
                    "El hotspot nativo requiere Windows 10 u 11."
            );
        }

        WinAPI api = Nativa.API;
        check(api.RoInitialize(1), "Inicializar WinRT");

        try (
                Objeto red = fab(
                        "Windows.Networking.Connectivity.NetworkInformation",
                        "5074F851-950D-4165-9C15-365619481EEA"
                );

                Objeto perfil = red.obj(7);

                Objeto fabrica = fab(
                        "Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager",
                        "5B235412-35F0-49E7-9B08-16D278FBAA42"
                )
        ) {

            int capacidad = fabrica.num(6, perfil.ptr);

            if (capacidad != 0) {
                throw new IllegalStateException(
                        "Conexión no compartible: " + capacidad(capacidad)
                );
            }

            try (Objeto gestor = fabrica.obj(7, perfil.ptr)) {

                int estado = gestor.num(8);

                if (estado == 1) {
                    return;
                }

                if (estado == 3) {
                    throw new IllegalStateException(
                            "El hotspot está cambiando de estado. "
                            + "Espera y vuelve a ejecutar."
                    );
                }

                try (
                        Objeto operacion = gestor.obj(11);

                        Objeto progreso = operacion.obj(
                                0,
                                new GUID(
                                        "00000036-0000-0000-C000-000000000046"
                                )
                        )
                ) {

                    esperar(progreso);

                    try (Objeto resultado = operacion.obj(8)) {

                        int codigo = resultado.num(6);

                        System.out.println(
                                "[WIFI] Resultado de activacion: " + codigo
                                + " (0=correcto, 9=ya estaba encendido)."
                        );

                        if (codigo != 0 && codigo != 9) {
                            throw new IllegalStateException(
                                    "Activación rechazada: " + explicar(codigo)
                            );
                        }
                    }
                }

                esperarEncendido(gestor, 15_000);
            }

        } finally {
            api.RoUninitialize();
        }
    }

    // Espera a que Windows confirme que el hotspot está encendido.
    static void esperarEncendido(Objeto gestor, long esperaMs)
            throws InterruptedException {

        long limite = System.nanoTime() + esperaMs * 1_000_000L;
        int estado = gestor.num(8);

        while (estado != 1 && System.nanoTime() < limite) {
            Thread.sleep(250);
            estado = gestor.num(8);
        }

        if (estado != 1) {
            throw new IllegalStateException(
                    "Hotspot sin confirmar tras "
                    + esperaMs / 1000
                    + " segundos. Estado=" + estado
                    + " (0=desconocido, 1=encendido,"
                    + " 2=apagado, 3=en transicion)."
                    + " Comprueba si Windows permite encenderlo"
                    + " desde Configuracion."
            );
        }
    }

    // Espera a que finalice la operación de activación.
    private static void esperar(Objeto progreso)
            throws InterruptedException {

        long limite = System.nanoTime() + 30_000_000_000L;

        try {

            while (progreso.num(7) == 0) {

                if (System.nanoTime() >= limite) {
                    throw new IllegalStateException(
                            "Tiempo agotado tras 30 segundos."
                    );
                }

                Thread.sleep(100);
            }

        } catch (InterruptedException | RuntimeException error) {
            progreso.call(9);
            throw error;
        }

        int estado = progreso.num(7);

        if (estado == 3) {
            check(progreso.num(8), "Activar hotspot");
        }

        if (estado != 1) {
            throw new IllegalStateException(
                    "Windows canceló o no completó la activación."
            );
        }
    }

    private static String capacidad(int codigo) {

        return switch (codigo) {
            case 1 ->
                "restringida por una política del equipo (1)";
            case 2 ->
                "el hardware o su controlador no lo permiten (2)";
            case 3 ->
                "restringida por el operador (3)";
            case 4 ->
                "los servicios de la cuenta no permiten compartir la conexión (4)";
            case 5 ->
                "falta una aplicación requerida por el operador (5)";
            case 7 ->
                "el sistema no concede la capacidad requerida (7)";
            default ->
                "código de Windows " + codigo;
        };
    }

    private static String explicar(int codigo) {

        return switch (codigo) {
            case 2 ->
                "adaptador de banda ancha apagado (2)";
            case 3 ->
                "adaptador Wi-Fi apagado (3)";
            case 4, 5 ->
                "falló la autorización del uso compartido (" + codigo + ")";
            case 6 ->
                "hay otra operación en curso (6)";
            case 7 ->
                "adaptador Bluetooth apagado (7)";
            case 8 ->
                "la conexión a Internet es limitada (8)";
            case 10 ->
                "restricción del adaptador inalámbrico (10)";
            case 11 ->
                "conflicto de banda inalámbrica (11)";
            default ->
                "código de Windows " + codigo;
        };
    }

    private static Objeto fab(String clase, String id) {

        PointerByReference nombre = new PointerByReference();

        check(
                Nativa.API.WindowsCreateString(
                        new WString(clase),
                        clase.length(),
                        nombre
                ),
                "Crear nombre WinRT"
        );

        try {

            PointerByReference resultado = new PointerByReference();

            check(
                    Nativa.API.RoGetActivationFactory(
                            nombre.getValue(),
                            new GUID(id),
                            resultado
                    ),
                    "Obtener " + clase
            );

            return new Objeto(resultado.getValue());

        } finally {
            Nativa.API.WindowsDeleteString(nombre.getValue());
        }
    }

    private static void check(int codigo, String accion) {

        if (codigo < 0) {
            throw new IllegalStateException(
                    accion + String.format(": HRESULT 0x%08X", codigo)
            );
        }
    }

    // Representa un objeto de las interfaces nativas de Windows.
    static class Objeto implements AutoCloseable {

        final Pointer ptr;

        Objeto(Pointer puntero) {

            if (puntero == null) {
                throw new IllegalStateException(
                        "Windows devolvió un objeto vacío. "
                        + "Comprueba la conexión a Internet."
                );
            }

            ptr = puntero;
        }

        int call(int indice, Object... parametros) {

            Object[] argumentos = new Object[parametros.length + 1];
            argumentos[0] = ptr;

            System.arraycopy(
                    parametros,
                    0,
                    argumentos,
                    1,
                    parametros.length
            );

            Pointer metodo = ptr
                    .getPointer(0)
                    .getPointer((long) indice * Native.POINTER_SIZE);

            return Function
                    .getFunction(metodo, Function.ALT_CONVENTION)
                    .invokeInt(argumentos);
        }

        void leer(int indice, Object salida, Object... parametros) {

            Object[] argumentos = java.util.Arrays.copyOf(
                    parametros,
                    parametros.length + 1
            );

            argumentos[parametros.length] = salida;

            check(
                    call(indice, argumentos),
                    "Método de Windows " + indice
            );
        }

        Objeto obj(int indice, Object... parametros) {

            PointerByReference resultado = new PointerByReference();

            leer(indice, resultado, parametros);

            return new Objeto(resultado.getValue());
        }

        int num(int indice, Object... parametros) {

            IntByReference resultado = new IntByReference();

            leer(indice, resultado, parametros);

            return resultado.getValue();
        }

        @Override
        public void close() {
            call(2);
        }
    }

    public interface WinAPI extends StdCallLibrary {

        int RoInitialize(int tipo);

        void RoUninitialize();

        int WindowsCreateString(
                WString texto,
                int longitud,
                PointerByReference resultado
        );

        int WindowsDeleteString(Pointer texto);

        int RoGetActivationFactory(
                Pointer clase,
                GUID iid,
                PointerByReference resultado
        );
    }
}