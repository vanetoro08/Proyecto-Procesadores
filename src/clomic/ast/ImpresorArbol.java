package clomic.ast;

import clomic.ast.Ast.*;
import java.util.List;

/**
 * Muestra el árbol que arma el parser, con un nodo por línea y sus hijos con más sangría.
 * Sirve para ver y explicar cómo el parser entendió un programa.
 *
 * Ejemplo, para (elemento x 20) (valorar (excede x 5) (emite "Greater than 5") (neutro ...)):
 *
 *     Declaracion x (line 1)
 *       Literal 20
 *     Si (line 3)
 *       Rama
 *         Operacion OP_MAYOR
 *           Variable x
 *           Literal 5
 *         Mostrar
 *           Literal "Greater than 5"
 *       Sino
 *         Mostrar
 *           Literal "Less than or equal to 5"
 *
 * Solo las sentencias de primer nivel muestran su línea, para que no sea tan cargado.
 */
public final class ImpresorArbol {

    private ImpresorArbol() {}

    public static String imprimir(List<Nodo> programa) {
        StringBuilder sb = new StringBuilder();
        for (Nodo sentencia : programa) {
            nodo(sb, sentencia, 0, true);
        }
        return sb.toString();
    }

    /** Escribe un nodo y, recursivamente, sus hijos con un nivel más de sangría. */
    private static void nodo(StringBuilder sb, Nodo n, int nivel, boolean conLinea) {
        String sufijo = conLinea ? " (line " + n.linea() + ")" : "";
        int h = nivel + 1;

        switch (n) {
            // ---------- Sentencias ----------
            case Declaracion d -> {
                linea(sb, nivel, "Declaracion " + d.nombre() + sufijo);
                if (d.valor() != null) nodo(sb, d.valor(), h, false);
            }
            case Asignacion a -> {
                linea(sb, nivel, "Asignacion" + sufijo);
                nodo(sb, a.destino(), h, false);
                nodo(sb, a.valor(), h, false);
            }
            case Si s -> {
                linea(sb, nivel, "Si" + sufijo);
                for (Rama r : s.ramas()) {
                    linea(sb, h, "Rama");
                    nodo(sb, r.condicion(), h + 1, false);
                    bloque(sb, r.cuerpo(), h + 1);
                }
                if (s.sino() != null) {
                    linea(sb, h, "Sino");
                    bloque(sb, s.sino(), h + 1);
                }
            }
            case Mientras m -> {
                linea(sb, nivel, "Mientras" + sufijo);
                nodo(sb, m.condicion(), h, false);
                bloque(sb, m.cuerpo(), h);
            }
            case Para p -> {
                linea(sb, nivel, "Para " + p.variable() + sufijo);
                nodo(sb, p.inicio(), h, false);
                nodo(sb, p.fin(), h, false);
                bloque(sb, p.cuerpo(), h);
            }
            case Romper r -> linea(sb, nivel, "Romper" + sufijo);
            case Continuar c -> linea(sb, nivel, "Continuar" + sufijo);
            case Mostrar m -> {
                linea(sb, nivel, "Mostrar" + sufijo);
                nodo(sb, m.valor(), h, false);
            }
            case DefFuncion f -> {
                linea(sb, nivel, "DefFuncion " + f.nombre() + " " + f.params() + sufijo);
                bloque(sb, f.cuerpo(), h);
            }
            case Retornar r -> {
                linea(sb, nivel, "Retornar" + sufijo);
                if (r.valor() != null) nodo(sb, r.valor(), h, false);
            }
            case DefClase c -> {
                linea(sb, nivel, "DefClase " + c.nombre() + sufijo);
                for (Declaracion atributo : c.atributos()) nodo(sb, atributo, h, false);
                if (c.constructor() != null) {
                    linea(sb, h, "Constructor " + c.constructor().params());
                    bloque(sb, c.constructor().cuerpo(), h + 1);
                }
                for (DefFuncion metodo : c.metodos()) nodo(sb, metodo, h, false);
            }
            case Intentar t -> {
                linea(sb, nivel, "Intentar" + sufijo);
                bloque(sb, t.cuerpo(), h);
                if (t.captura() != null) {
                    linea(sb, h, "Neutraliza " + t.varError());
                    bloque(sb, t.captura(), h + 1);
                }
                if (t.finalmente() != null) {
                    linea(sb, h, "Residuo");
                    bloque(sb, t.finalmente(), h + 1);
                }
            }
            case Lanzar l -> {
                linea(sb, nivel, "Lanzar" + sufijo);
                nodo(sb, l.valor(), h, false);
            }

            // ---------- Expresiones ----------
            case Literal l -> linea(sb, nivel, "Literal " + literal(l.valor()) + sufijo);
            case Variable v -> linea(sb, nivel, "Variable " + v.nombre() + sufijo);
            case Este e -> linea(sb, nivel, "Este (molecula)" + sufijo);
            case Acceso a -> {
                linea(sb, nivel, "Acceso ." + a.atributo() + sufijo);
                nodo(sb, a.objeto(), h, false);
            }
            case Indice i -> {
                linea(sb, nivel, "Indice" + sufijo);
                nodo(sb, i.lista(), h, false);
                nodo(sb, i.posicion(), h, false);
            }
            case Tamano t -> {
                linea(sb, nivel, "Tamano" + sufijo);
                nodo(sb, t.lista(), h, false);
            }
            case ListaLit l -> {
                linea(sb, nivel, "Lista" + sufijo);
                bloque(sb, l.elementos(), h);
            }
            case Operacion o -> {
                linea(sb, nivel, "Operacion " + o.op() + sufijo);
                bloque(sb, o.operandos(), h);
            }
            case Leer l -> {
                linea(sb, nivel, "Leer" + sufijo);
                if (l.mensaje() != null) nodo(sb, l.mensaje(), h, false);
            }
            case Nuevo nu -> {
                linea(sb, nivel, "Nuevo " + nu.clase() + sufijo);
                bloque(sb, nu.args(), h);
            }
            case Llamada ll -> {
                linea(sb, nivel, "Llamada" + sufijo);
                nodo(sb, ll.funcion(), h, false);
                bloque(sb, ll.args(), h);
            }
            default -> linea(sb, nivel, n.getClass().getSimpleName() + sufijo);
        }
    }

    private static void bloque(StringBuilder sb, List<? extends Nodo> nodos, int nivel) {
        for (Nodo n : nodos) {
            nodo(sb, n, nivel, false);
        }
    }

    private static void linea(StringBuilder sb, int nivel, String texto) {
        sb.append("  ".repeat(nivel)).append(texto).append('\n');
    }

    /** Strings entre comillas para distinguir "5" de 5; booleanos y vacio con sus palabras de Clomic. */
    private static String literal(Object v) {
        if (v == null) return "vacio";
        if (v instanceof String s) return "\"" + s + "\"";
        if (v instanceof Boolean b) return b ? "polar" : "apolar";
        return v.toString();
    }
}
