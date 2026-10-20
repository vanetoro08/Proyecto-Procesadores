package clomic.ast;

import clomic.lexer.TipoToken;
import java.util.List;

/**
 * Nodos del árbol (contrato del documento de diseño).
 * Van agrupados como records anidados para no tener 30 archivos sueltos;
 * se usan con: import clomic.ast.Ast.*;
 */
public final class Ast {
    private Ast() {}

    // ---------- Sentencias ----------
    public record Declaracion(String nombre, Nodo valor, int linea) implements Nodo {}      // valor puede ser null
    public record Asignacion(Nodo destino, Nodo valor, int linea) implements Nodo {}        // destino: Variable, Acceso o Indice
    public record Rama(Nodo condicion, List<Nodo> cuerpo) {}
    public record Si(List<Rama> ramas, List<Nodo> sino, int linea) implements Nodo {}      // ramas[0] es el valorar; sino puede ser null
    public record Mientras(Nodo condicion, List<Nodo> cuerpo, int linea) implements Nodo {}
    public record Para(String variable, Nodo inicio, Nodo fin, List<Nodo> cuerpo, int linea) implements Nodo {}
    public record Romper(int linea) implements Nodo {}
    public record Continuar(int linea) implements Nodo {}
    public record Mostrar(Nodo valor, int linea) implements Nodo {}
    public record DefFuncion(String nombre, List<String> params, List<Nodo> cuerpo, int linea) implements Nodo {}
    public record Retornar(Nodo valor, int linea) implements Nodo {}
    public record DefClase(String nombre, List<Declaracion> atributos, DefFuncion constructor,
                           List<DefFuncion> metodos, int linea) implements Nodo {}         // constructor puede ser null
    public record Intentar(List<Nodo> cuerpo, String varError, List<Nodo> captura,
                           List<Nodo> finalmente, int linea) implements Nodo {}
    public record Lanzar(Nodo valor, int linea) implements Nodo {}

    // ---------- Expresiones ----------
    public record Literal(Object valor, int linea) implements Nodo {}       // Long, Double, String, Boolean o null
    public record Variable(String nombre, int linea) implements Nodo {}
    public record Este(int linea) implements Nodo {}                         // molecula
    public record Acceso(Nodo objeto, String atributo, int linea) implements Nodo {}
    public record Indice(Nodo lista, Nodo posicion, int linea) implements Nodo {}
    public record Tamano(Nodo lista, int linea) implements Nodo {}           // masa
    public record ListaLit(List<Nodo> elementos, int linea) implements Nodo {}
    public record Operacion(TipoToken op, List<Nodo> operandos, int linea) implements Nodo {}
    public record Leer(Nodo mensaje, int linea) implements Nodo {}           // mensaje puede ser null
    public record Nuevo(String clase, List<Nodo> args, int linea) implements Nodo {}
    public record Llamada(Nodo funcion, List<Nodo> args, int linea) implements Nodo {}  // funcion: Variable o Acceso
}
