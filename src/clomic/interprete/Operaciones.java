package clomic.interprete;

import clomic.errores.ErrorEjecucion;

/**
 * PROVISIONAL: versión básica para que el intérprete se pueda probar hoy.
 * La persona 4 la completa y la prueba, SIN cambiar los nombres ni los parámetros de los métodos.
 *
 * Valores en Clomic: Long (entero), Double (real), String, Boolean y null (vacio).
 */
public final class Operaciones {
    private Operaciones() {}

    // ---------- Aritmética ----------

    /** + : si alguno es string, concatena; si no, suma números. */
    public static Object sumar(Object a, Object b, int linea) {
        if (a instanceof String || b instanceof String) {
            return aTexto(a) + aTexto(b);
        }
        if (a instanceof Long x && b instanceof Long y) return x + y;
        return real(a, linea) + real(b, linea);
    }

    public static Object restar(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) return x - y;
        return real(a, linea) - real(b, linea);
    }

    public static Object multiplicar(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) return x * y;
        return real(a, linea) * real(b, linea);
    }

    /** / siempre da real: (/ 7 2) = 3.5 */
    public static Object dividir(Object a, Object b, int linea) {
        double divisor = real(b, linea);
        if (divisor == 0) throw new ErrorEjecucion(linea, "division by zero");
        return real(a, linea) / divisor;
    }

    public static Object modulo(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) {
            if (y == 0) throw new ErrorEjecucion(linea, "division by zero");
            return x % y;
        }
        double divisor = real(b, linea);
        if (divisor == 0) throw new ErrorEjecucion(linea, "division by zero");
        return real(a, linea) % divisor;
    }

    /** (- x) con un solo operando. */
    public static Object negar(Object a, int linea) {
        if (a instanceof Long x) return -x;
        return -real(a, linea);
    }

    // ---------- Comparaciones ----------

    /** Negativo si a < b, 0 si son iguales, positivo si a > b. Para excede, bajo, excede=, bajo=. */
    public static int comparar(Object a, Object b, int linea) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(real(a, linea), real(b, linea));
        }
        if (a instanceof String x && b instanceof String y) {
            return x.compareTo(y);
        }
        throw new ErrorEjecucion(linea, "cannot compare " + tipo(a) + " with " + tipo(b));
    }

    /** isotopo: 2 y 2.0 cuentan como iguales. */
    public static boolean sonIguales(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return x.doubleValue() == y.doubleValue();
        }
        return a == null ? b == null : a.equals(b);
    }

    // ---------- Conversiones ----------

    /** Cómo se ve un valor al mostrarlo o concatenarlo. Los enteros salen sin .0 porque son Long. */
    public static String aTexto(Object v) {
        if (v == null) return "vacio";
        if (v instanceof Boolean b) return b ? "polar" : "apolar";
        return v.toString();
    }

    /** Lo que se escribe en absorbe: si parece número se convierte; si no, queda string. */
    public static Object convertirEntrada(String texto) {
        try { return Long.parseLong(texto); } catch (NumberFormatException ignorado) { }
        try { return Double.parseDouble(texto); } catch (NumberFormatException ignorado) { }
        return texto;
    }

    /** Para los límites de cristaliza, que tienen que ser enteros. */
    public static long entero(Object v, int linea) {
        if (v instanceof Long x) return x;
        throw new ErrorEjecucion(linea, "expected an integer but got " + tipo(v));
    }

    // ---------- Ayudas ----------

    private static double real(Object v, int linea) {
        if (v instanceof Number n) return n.doubleValue();
        throw new ErrorEjecucion(linea, "expected a number but got " + tipo(v));
    }

    private static String tipo(Object v) {
        if (v == null) return "vacio";
        if (v instanceof Long) return "integer";
        if (v instanceof Double) return "real";
        if (v instanceof String) return "string";
        if (v instanceof Boolean) return "boolean";
        return v.getClass().getSimpleName();
    }
}
