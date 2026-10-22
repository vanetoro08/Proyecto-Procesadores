package clomic.interprete;

import clomic.errores.ErrorEjecucion;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Pruebas de Operaciones. Cada prueba imprime OK o FALLA con lo esperado y lo obtenido.
 *
 * Ejecutar:  java -cp out clomic.interprete.PruebaOperaciones
 */

public class PruebaOperaciones {
    private static int total = 0;
    private static int fallas = 0;

    public static void main(String[] args) {
        System.out.println("== Aritmética ==");
        igual("sumar(5, \"hola\")", "5hola", Operaciones.sumar(5L, "hola", 1));
        igual("sumar(\"hola\", 5)", "hola5", Operaciones.sumar("hola", 5L, 1));
        igual("sumar(\"a\", \"b\")", "ab", Operaciones.sumar("a", "b", 1));
        igual("sumar(\"x\", null)", "xvacio", Operaciones.sumar("x", null, 1));
        igual("sumar(\"ok: \", true)", "ok: polar", Operaciones.sumar("ok: ", true, 1));
        igual("sumar(2, 3)", 5L, Operaciones.sumar(2L, 3L, 1));
        igual("sumar(2, 0.5)", 2.5, Operaciones.sumar(2L, 0.5, 1));
        igual("sumar(0.5, 2)", 2.5, Operaciones.sumar(0.5, 2L, 1));
        error("sumar(true, 1)", "expected a number but got boolean", () -> Operaciones.sumar(true, 1L, 3));

        igual("restar(10, 4)", 6L, Operaciones.restar(10L, 4L, 1));
        igual("restar(10, 0.5)", 9.5, Operaciones.restar(10L, 0.5, 1));
        error("restar(\"hola\", 1)", "expected a number but got string", () -> Operaciones.restar("hola", 1L, 7));

        igual("multiplicar(3, 4)", 12L, Operaciones.multiplicar(3L, 4L, 1));
        igual("multiplicar(3, 0.5)", 1.5, Operaciones.multiplicar(3L, 0.5, 1));
        error("multiplicar(1, vacio)", "expected a number but got vacio", () -> Operaciones.multiplicar(1L, null, 1));

        igual("dividir(7, 2)", 3.5, Operaciones.dividir(7L, 2L, 1));
        igual("dividir(10, 2) es real", 5.0, Operaciones.dividir(10L, 2L, 1));
        igual("dividir(1.0, 4)", 0.25, Operaciones.dividir(1.0, 4L, 1));
        error("dividir(1, 0)", "division by zero", () -> Operaciones.dividir(1L, 0L, 5));
        error("dividir(1, 0.0)", "division by zero", () -> Operaciones.dividir(1L, 0.0, 5));
        error("dividir(\"hola\", 0)", "expected a number but got string", () -> Operaciones.dividir("hola", 0L, 5));

        igual("modulo(7, 3)", 1L, Operaciones.modulo(7L, 3L, 1));
        igual("modulo(-7, 3)", -1L, Operaciones.modulo(-7L, 3L, 1));
        igual("modulo(7.5, 2)", 1.5, Operaciones.modulo(7.5, 2L, 1));
        error("modulo(5, 0)", "division by zero", () -> Operaciones.modulo(5L, 0L, 2));
        error("modulo(5.0, 0)", "division by zero", () -> Operaciones.modulo(5.0, 0L, 2));
        error("modulo(\"a\", 2)", "expected a number but got string", () -> Operaciones.modulo("a", 2L, 2));

        igual("negar(5)", -5L, Operaciones.negar(5L, 1));
        igual("negar(-5)", 5L, Operaciones.negar(-5L, 1));
        igual("negar(2.5)", -2.5, Operaciones.negar(2.5, 1));
        error("negar(\"hola\")", "expected a number but got string", () -> Operaciones.negar("hola", 4));

        System.out.println("== Mensaje completo del error ==");
        try {
            Operaciones.restar("hola", 1L, 12);
            falla("mensaje de error", "excepción", "no lanzó nada");
        } catch (ErrorEjecucion e) {
            igual("mensaje de error", "Runtime error on line 12: expected a number but got string", e.getMessage());
            igual("linea() del error", 12, e.linea());
        }

        System.out.println("== Comparaciones ==");
        verdadero("comparar(1, 2) < 0", Operaciones.comparar(1L, 2L, 1) < 0);
        verdadero("comparar(2, 2) == 0", Operaciones.comparar(2L, 2L, 1) == 0);
        verdadero("comparar(3, 2) > 0", Operaciones.comparar(3L, 2L, 1) > 0);
        verdadero("comparar(2, 2.5) < 0", Operaciones.comparar(2L, 2.5, 1) < 0);
        verdadero("comparar(2.5, 2) > 0", Operaciones.comparar(2.5, 2L, 1) > 0);
        verdadero("comparar(2, 2.0) == 0", Operaciones.comparar(2L, 2.0, 1) == 0);
        verdadero("comparar(0.0, -0.0) == 0", Operaciones.comparar(0.0, -0.0, 1) == 0);
        verdadero("comparar(\"a\", \"b\") < 0", Operaciones.comparar("a", "b", 1) < 0);
        verdadero("comparar(\"b\", \"b\") == 0", Operaciones.comparar("b", "b", 1) == 0);
        verdadero("comparar(Long grande, Long grande+1) < 0",
                Operaciones.comparar(9007199254740993L, 9007199254740994L, 1) < 0);
        error("comparar(2, \"2\")", "cannot compare integer with string", () -> Operaciones.comparar(2L, "2", 6));
        error("comparar(\"a\", 1.5)", "cannot compare string with real", () -> Operaciones.comparar("a", 1.5, 6));
        error("comparar(true, false)", "cannot compare boolean with boolean", () -> Operaciones.comparar(true, false, 6));
        error("comparar(vacio, 1)", "cannot compare vacio with integer", () -> Operaciones.comparar(null, 1L, 6));

        verdadero("sonIguales(2, 2.0)", Operaciones.sonIguales(2L, 2.0));
        verdadero("sonIguales(2.0, 2)", Operaciones.sonIguales(2.0, 2L));
        verdadero("sonIguales(2, 2)", Operaciones.sonIguales(2L, 2L));
        falso("sonIguales(2, 3)", Operaciones.sonIguales(2L, 3L));
        verdadero("sonIguales(\"a\", \"a\")", Operaciones.sonIguales("a", "a"));
        falso("sonIguales(\"a\", \"b\")", Operaciones.sonIguales("a", "b"));
        falso("sonIguales(2, \"2\")", Operaciones.sonIguales(2L, "2"));
        verdadero("sonIguales(true, true)", Operaciones.sonIguales(true, true));
        falso("sonIguales(true, false)", Operaciones.sonIguales(true, false));
        verdadero("sonIguales(vacio, vacio)", Operaciones.sonIguales(null, null));
        falso("sonIguales(vacio, 0)", Operaciones.sonIguales(null, 0L));
        falso("sonIguales(0, vacio)", Operaciones.sonIguales(0L, null));
        falso("sonIguales(vacio, \"vacio\")", Operaciones.sonIguales(null, "vacio"));
        falso("sonIguales(vacio, apolar)", Operaciones.sonIguales(null, false));
        verdadero("sonIguales([1 2], [1 2.0])", Operaciones.sonIguales(List.of(1L, 2L), List.of(1L, 2.0)));
        falso("sonIguales([1 2], [1 3])", Operaciones.sonIguales(List.of(1L, 2L), List.of(1L, 3L)));
        falso("sonIguales([1 2], [1])", Operaciones.sonIguales(List.of(1L, 2L), List.of(1L)));

        System.out.println("== Conversiones ==");
        igual("aTexto(true)", "polar", Operaciones.aTexto(true));
        igual("aTexto(false)", "apolar", Operaciones.aTexto(false));
        igual("aTexto(vacio)", "vacio", Operaciones.aTexto(null));
        igual("aTexto(5)", "5", Operaciones.aTexto(5L));
        igual("aTexto(-5)", "-5", Operaciones.aTexto(-5L));
        igual("aTexto(3.5)", "3.5", Operaciones.aTexto(3.5));
        igual("aTexto(5.0)", "5.0", Operaciones.aTexto(5.0));
        igual("aTexto(10000000.0) sin notación científica", "10000000.0", Operaciones.aTexto(1.0E7));
        igual("aTexto(0.00001) sin notación científica", "0.00001", Operaciones.aTexto(0.00001));
        igual("aTexto(\"hola\")", "hola", Operaciones.aTexto("hola"));
        igual("aTexto([4 8 15])", "[4 8 15]", Operaciones.aTexto(List.of(4L, 8L, 15L)));
        igual("aTexto([])", "[]", Operaciones.aTexto(List.of()));
        igual("aTexto(lista mezclada)", "[1 2.5 hola polar vacio]",
                Operaciones.aTexto(Arrays.asList(1L, 2.5, "hola", true, null)));
        igual("aTexto(lista anidada)", "[1 [2 3]]", Operaciones.aTexto(List.of(1L, List.of(2L, 3L))));

        igual("convertirEntrada(\"25\")", 25L, Operaciones.convertirEntrada("25"));
        igual("convertirEntrada(\"-3\")", -3L, Operaciones.convertirEntrada("-3"));
        igual("convertirEntrada(\"3.5\")", 3.5, Operaciones.convertirEntrada("3.5"));
        igual("convertirEntrada(\"-0.25\")", -0.25, Operaciones.convertirEntrada("-0.25"));
        igual("convertirEntrada(\"hola\")", "hola", Operaciones.convertirEntrada("hola"));
        igual("convertirEntrada(\"\")", "", Operaciones.convertirEntrada(""));
        igual("convertirEntrada(\"12abc\")", "12abc", Operaciones.convertirEntrada("12abc"));
        igual("convertirEntrada(\"NaN\") sigue siendo texto", "NaN", Operaciones.convertirEntrada("NaN"));
        igual("convertirEntrada(\"Infinity\") sigue siendo texto", "Infinity", Operaciones.convertirEntrada("Infinity"));
        igual("convertirEntrada(\"2f\") sigue siendo texto", "2f", Operaciones.convertirEntrada("2f"));
        igual("convertirEntrada(\"1e5\") sigue siendo texto", "1e5", Operaciones.convertirEntrada("1e5"));

        igual("entero(7)", 7L, Operaciones.entero(7L, 1));
        error("entero(2.0)", "expected an integer but got real", () -> Operaciones.entero(2.0, 9));
        error("entero(\"1\")", "expected an integer but got string", () -> Operaciones.entero("1", 9));
        error("entero(vacio)", "expected an integer but got vacio", () -> Operaciones.entero(null, 9));

        System.out.println("== tamano (masa) ==");
        igual("tamano([4 8 15 16])", 4L, Operaciones.tamano(List.of(4L, 8L, 15L, 16L), 1));
        igual("tamano([])", 0L, Operaciones.tamano(List.of(), 1));
        igual("tamano(\"hola\")", 4L, Operaciones.tamano("hola", 1));
        igual("tamano(\"\")", 0L, Operaciones.tamano("", 1));
        error("tamano(5)", "expected a list or string but got integer", () -> Operaciones.tamano(5L, 8));
        error("tamano(vacio)", "expected a list or string but got vacio", () -> Operaciones.tamano(null, 8));
        error("tamano(true)", "expected a list or string but got boolean", () -> Operaciones.tamano(true, 8));

        System.out.println();
        if (fallas == 0) {
            System.out.println("Resultado: " + total + " pruebas, todas OK");
        } else {
            System.out.println("Resultado: " + fallas + " FALLA(S) de " + total + " pruebas");
            System.exit(1);
        }
    }

    // Ayudas

    // Compara con equals; Long y Double distintos NO son iguales aquí (así se detecta el tipo)
    private static void igual(String nombre, Object esperado, Object obtenido) {
        boolean ok = esperado.equals(obtenido);
        if (ok) ok(nombre, esperado);
        else falla(nombre, mostrar(esperado), mostrar(obtenido));
    }

    private static void verdadero(String nombre, boolean obtenido) {
        igual(nombre, true, obtenido);
    }

    private static void falso(String nombre, boolean obtenido) {
        igual(nombre, false, obtenido);
    }

    // La operación tiene que lanzar ErrorEjecucion con ese detalle y la línea que se le pasó
    private static void error(String nombre, String detalleEsperado, Supplier<Object> operacion) {
        try {
            Object r = operacion.get();
            falla(nombre, "error: " + detalleEsperado, "no lanzó error, devolvió " + mostrar(r));
        } catch (ErrorEjecucion e) {
            if (e.detalle().equals(detalleEsperado)) ok(nombre, "error: " + detalleEsperado);
            else falla(nombre, "error: " + detalleEsperado, "error: " + e.detalle());
        } catch (RuntimeException e) {
            falla(nombre, "ErrorEjecucion: " + detalleEsperado, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void ok(String nombre, Object valor) {
        total++;
        System.out.println("OK    " + nombre + " = " + mostrar(valor));
    }

    private static void falla(String nombre, String esperado, String obtenido) {
        total++;
        fallas++;
        System.out.println("FALLA " + nombre + " -> esperado: " + esperado + " | obtenido: " + obtenido);
    }

    // Muestra el tipo Java para distinguir 2 (Long) de 2.0 (Double) en los mensajes
    private static String mostrar(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + s + "\"";
        if (v instanceof Long) return v + "L";
        return v.toString();
    }
}