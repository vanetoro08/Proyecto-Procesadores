package clomic.interprete;

import clomic.errores.ErrorEjecucion;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Maneja las operaciones, conversiones y validaciones de los valores en el lenguaje Clomic.
 * Tipos soportados: Long (entero), Double (real), String, Boolean, null (vacío) y Listas.
 * Todos los errores en tiempo de ejecución se lanzan como un ErrorEjecucion.
 */

public final class Operaciones {

    private Operaciones() {}

    // Patrones para identificar números escritos como texto por el usuario
    private static final Pattern ENTERO = Pattern.compile("[+-]?\\d+");
    private static final Pattern REAL = Pattern.compile("[+-]?\\d+\\.\\d+");

    // OPERACIONES ARITMÉTICAS

    /**
     * Suma dos valores. Si alguno es texto, concatena ambos; si son enteros, da entero; 
     * si interviene algún número real, el resultado es real.
     */
    public static Object sumar(Object a, Object b, int linea) {
        if (a instanceof String || b instanceof String) {
            return aTexto(a) + aTexto(b);
        }
        if (a instanceof Long x && b instanceof Long y) return x + y;
        return real(a, linea) + real(b, linea);
    }

    // Resta dos valores. Si ambos son enteros da entero; si hay reales, da real
    public static Object restar(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) return x - y;
        return real(a, linea) - real(b, linea);
    }

    // Multiplica dos valores. Si ambos son enteros da entero; si hay reales, da real
    public static Object multiplicar(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) return x * y;
        return real(a, linea) * real(b, linea);
    }

    // Divide dos valores. El resultado siempre es un número real. Lanza error si se divide entre cero.
    public static Object dividir(Object a, Object b, int linea) {
        double dividendo = real(a, linea);
        double divisor = real(b, linea);
        if (divisor == 0) throw new ErrorEjecucion(linea, "division by zero");
        return dividendo / divisor;
    }

    // Calcula el módulo (resto de la división). Lanza error si el divisor es cero
    public static Object modulo(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) {
            if (y == 0) throw new ErrorEjecucion(linea, "division by zero");
            return x % y;
        }
        double dividendo = real(a, linea);
        double divisor = real(b, linea);
        if (divisor == 0) throw new ErrorEjecucion(linea, "division by zero");
        return dividendo % divisor;
    }

    // Cambia el signo de un número (por ejemplo, de 5 pasa a -5)
    public static Object negar(Object a, int linea) {
        if (a instanceof Long x) return -x;
        return -real(a, linea);
    }

    // COMPARACIONES

    /**
     * Compara dos valores. Retorna negativo si a < b, cero si son iguales, o positivo si a > b.
     * Solo permite comparar números entre sí o textos entre sí.
     */
    public static int comparar(Object a, Object b, int linea) {
        if (a instanceof Long x && b instanceof Long y) {
            return Long.compare(x, y); // Mantiene precisión sin convertir a double
        }
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
        if (a instanceof String x && b instanceof String y) {
            return x.compareTo(y);
        }
        throw new ErrorEjecucion(linea, "cannot compare " + tipo(a) + " with " + tipo(b));
    }

    /**
     * Verifica si dos valores son estrictamente equivalentes.
     * Los enteros y reales equivalentes (ej. 2 y 2.0) se consideran iguales. Las listas se comparan elemento a elemento.
     */
    public static boolean sonIguales(Object a, Object b) {
        if (a == null || b == null) return a == null && b == null;
        if (a instanceof Long x && b instanceof Long y) return x.longValue() == y.longValue();
        if (a instanceof Number x && b instanceof Number y) {
            return x.doubleValue() == y.doubleValue();
        }
        if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) return false;
            for (int i = 0; i < x.size(); i++) {
                if (!sonIguales(x.get(i), y.get(i))) return false;
            }
            return true;
        }
        return a.equals(b);
    }

    // CONVERSIONES Y FORMATO

    /**
     * Convierte cualquier valor a su representación en texto legible para Clomic:
     * - null -> "vacio"
     * - Boolean -> "polar" (true) o "apolar" (false)
     * - Listas -> Se muestran con espacios (ej. [4 8 15])
     */
    public static String aTexto(Object v) {
        if (v == null) return "vacio";
        if (v instanceof Boolean b) return b ? "polar" : "apolar";
        if (v instanceof Double d) return realATexto(d);
        if (v instanceof List<?> lista) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(aTexto(lista.get(i)));
            }
            return sb.append(']').toString();
        }
        return v.toString();
    }

    // Convierte una entrada de texto en su tipo de dato correspondiente (Entero, Real o Texto plano).
    public static Object convertirEntrada(String texto) {
        if (texto == null) return null;
        if (ENTERO.matcher(texto).matches()) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException demasiadoGrande) {
                return Double.parseDouble(texto); // Si es muy grande para un Long, pasa a ser real
            }
        }
        if (REAL.matcher(texto).matches()) return Double.parseDouble(texto);
        return texto;
    }

    // El valor debe ser un número entero, arrojará un error en caso contrario
    public static long entero(Object v, int linea) {
        if (v instanceof Long x) return x;
        throw new ErrorEjecucion(linea, "expected an integer but got " + tipo(v));
    }

    // TAMAÑO / MASA

    // Retorna el tamaño de una lista o la cantidad de caracteres de un texto
    public static long tamano(Object v, int linea) {
        if (v instanceof List<?> lista) return lista.size();
        if (v instanceof String s) return s.length();
        throw new ErrorEjecucion(linea, "expected a list or string but got " + tipo(v));
    }

    // MÉTODOS AUXILIARES PRIVADOS

    private static double real(Object v, int linea) {
        if (v instanceof Number n) return n.doubleValue();
        throw new ErrorEjecucion(linea, "expected a number but got " + tipo(v));
    }

    // Formatea un número real a texto de forma limpia, evitando notación científica (ej. "5.0", "3.5")
    private static String realATexto(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return Double.toString(d);
        String texto = new BigDecimal(Double.toString(d)).stripTrailingZeros().toPlainString();
        return texto.contains(".") ? texto : texto + ".0";
    }

    // Devuelve el nombre del tipo de dato en formato legible para mensajes de error
    private static String tipo(Object v) {
        if (v == null) return "vacio";
        if (v instanceof Long) return "integer";
        if (v instanceof Double) return "real";
        if (v instanceof String) return "string";
        if (v instanceof Boolean) return "boolean";
        if (v instanceof List) return "list";
        return v.getClass().getSimpleName();
    }
}