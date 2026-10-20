package clomic.parser;

import clomic.ast.Nodo;
import clomic.ast.Ast.*;
import clomic.errores.ErrorSintactico;
import clomic.lexer.TipoToken;
import clomic.lexer.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static clomic.lexer.TipoToken.*;

/**
 * Analizador sintáctico de Clomic: convierte la lista de tokens en el árbol (List<Nodo>).
 *
 * Es un parser de DESCENSO RECURSIVO: hay un método por cada regla de la gramática
 * (sección 5 del documento de diseño), y cada método se llama a sí mismo o a los otros
 * cuando la regla contiene otras reglas. Por ejemplo, condicional() llama a expresion()
 * para la condición y a sentencia() para el cuerpo.
 *
 * Además verifica el CONTEXTO (sección 7): produce solo dentro de funciones, precipita y
 * sublima solo dentro de ciclos, molecula solo dentro de un compuesto. Eso no se puede
 * expresar en la EBNF, así que se lleva la cuenta con tres contadores.
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    // Contexto: ¿dentro de cuántas funciones, ciclos y clases estamos?
    private int dentroFuncion = 0;
    private int dentroCiclo = 0;
    private int dentroClase = 0;

    private static final Set<TipoToken> ARITMETICOS = Set.of(OP_SUMA, OP_RESTA, OP_MULT, OP_DIV, OP_MOD);
    private static final Set<TipoToken> RELACIONALES =
            Set.of(OP_MAYOR, OP_MENOR, OP_MAYOR_IGUAL, OP_MENOR_IGUAL, OP_IGUAL, OP_DISTINTO);

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // =====================================================================
    //  PROGRAMA Y SENTENCIAS
    // =====================================================================

    /** programa = { sentencia } EOF */
    public List<Nodo> parsearPrograma() {
        List<Nodo> programa = new ArrayList<>();
        while (!es(EOF)) {
            programa.add(sentencia());
        }
        return programa;
    }

    /**
     * Mira el "(" y la palabra que sigue para decidir qué regla aplicar.
     * Si no es una palabra de sentencia, lo que hay es una expresión (por ejemplo una llamada).
     */
    private Nodo sentencia() {
        if (es(LPAREN)) {
            switch (mirar(1).tipo()) {
                case KW_ELEMENTO:   return declaracion();
                case KW_ENLACE:     return asignacion();
                case KW_VALORAR:    return condicional();
                case KW_CATALIZADOR: return mientras();
                case KW_CRISTALIZA: return para();
                case KW_PRECIPITA:  return romperOContinuar(true);
                case KW_SUBLIMA:    return romperOContinuar(false);
                case KW_EMITE:      return mostrar();
                case KW_REACCION:   return defFuncion();
                case KW_PRODUCE:    return retornar();
                case KW_COMPUESTO:  return defClase();
                case KW_ENSAYA:     return intentar();
                case KW_EXPLOTA:    return lanzar();
                // Palabras que solo existen dentro de otra estructura
                case KW_REVALORAR, KW_NEUTRO:
                    throw error(mirar(1), "'" + mirar(1).lexema() + "' outside 'valorar'");
                case KW_NEUTRALIZA, KW_RESIDUO:
                    throw error(mirar(1), "'" + mirar(1).lexema() + "' outside 'ensaya'");
                case KW_NUCLEO:
                    throw error(mirar(1), "'nucleo' outside 'compuesto'");
                default:
                    break;
            }
        }
        return expresion();
    }

    /** declaracion = "(" "elemento" ID [ expresion ] ")" */
    private Declaracion declaracion() {
        int linea = abrir(KW_ELEMENTO);
        String nombre = consumir(ID, "expected a variable name after 'elemento'").lexema();
        Nodo valor = es(RPAREN) ? null : expresion();
        cerrar();
        return new Declaracion(nombre, valor, linea);
    }

    /** asignacion = "(" "enlace" destino expresion ")"   con destino = ID | acceso | indice */
    private Asignacion asignacion() {
        int linea = abrir(KW_ENLACE);
        Token inicioDestino = actual();
        Nodo destino = expresion();
        if (!(destino instanceof Variable || destino instanceof Acceso || destino instanceof Indice)) {
            throw error(inicioDestino, "invalid assignment target");
        }
        Nodo valor = expresion();
        cerrar();
        return new Asignacion(destino, valor, linea);
    }

    /**
     * condicional = "(" "valorar" expresion { sentencia } { rama_revalorar } [ rama_neutro ] ")"
     * El cuerpo del valorar termina cuando aparece "(" seguido de revalorar o neutro, o el ")".
     */
    private Si condicional() {
        int linea = abrir(KW_VALORAR);
        List<Rama> ramas = new ArrayList<>();
        Nodo condicion = expresion();
        ramas.add(new Rama(condicion, cuerpoHasta(KW_REVALORAR, KW_NEUTRO)));

        while (es(LPAREN) && mirar(1).tipo() == KW_REVALORAR) {
            abrir(KW_REVALORAR);
            Nodo cond = expresion();
            List<Nodo> cuerpo = cuerpoHasta(KW_REVALORAR, KW_NEUTRO);
            cerrar();
            ramas.add(new Rama(cond, cuerpo));
        }

        List<Nodo> sino = null;
        if (es(LPAREN) && mirar(1).tipo() == KW_NEUTRO) {
            abrir(KW_NEUTRO);
            sino = cuerpoHasta();
            cerrar();
        }
        cerrar(); // si aquí llega otro revalorar después del neutro, falla con "expected ')'"
        return new Si(ramas, sino, linea);
    }

    /** mientras = "(" "catalizador" expresion { sentencia } ")" */
    private Mientras mientras() {
        int linea = abrir(KW_CATALIZADOR);
        Nodo condicion = expresion();
        dentroCiclo++;
        List<Nodo> cuerpo = cuerpoHasta();
        dentroCiclo--;
        cerrar();
        return new Mientras(condicion, cuerpo, linea);
    }

    /** para = "(" "cristaliza" "[" ID expresion expresion "]" { sentencia } ")" */
    private Para para() {
        int linea = abrir(KW_CRISTALIZA);
        consumir(LBRACKET, "expected '[' after 'cristaliza'");
        String variable = consumir(ID, "expected a variable name in 'cristaliza'").lexema();
        Nodo inicio = expresion();
        Nodo fin = expresion();
        consumir(RBRACKET, "expected ']'");
        dentroCiclo++;
        List<Nodo> cuerpo = cuerpoHasta();
        dentroCiclo--;
        cerrar();
        return new Para(variable, inicio, fin, cuerpo, linea);
    }

    /** romper = "(" "precipita" ")"   continuar = "(" "sublima" ")" */
    private Nodo romperOContinuar(boolean esRomper) {
        Token palabra = mirar(1);
        int linea = abrir(esRomper ? KW_PRECIPITA : KW_SUBLIMA);
        if (dentroCiclo == 0) {
            throw error(palabra, "'" + palabra.lexema() + "' outside a loop");
        }
        cerrar();
        return esRomper ? new Romper(linea) : new Continuar(linea);
    }

    /** mostrar = "(" "emite" expresion ")" */
    private Mostrar mostrar() {
        int linea = abrir(KW_EMITE);
        Nodo valor = expresion();
        cerrar();
        return new Mostrar(valor, linea);
    }

    /** def_funcion = "(" "reaccion" ID parametros { sentencia } ")" */
    private DefFuncion defFuncion() {
        int linea = abrir(KW_REACCION);
        String nombre = consumir(ID, "expected a function name after 'reaccion'").lexema();
        List<String> params = parametros();
        List<Nodo> cuerpo = cuerpoDeFuncion();
        cerrar();
        return new DefFuncion(nombre, params, cuerpo, linea);
    }

    /** parametros = "[" { ID } "]" */
    private List<String> parametros() {
        consumir(LBRACKET, "expected '[' with the parameters");
        List<String> params = new ArrayList<>();
        while (!es(RBRACKET)) {
            params.add(consumir(ID, "expected a parameter name").lexema());
        }
        consumir(RBRACKET, "expected ']'");
        return params;
    }

    /**
     * El cuerpo de una función es contexto nuevo: dentro de ella sí vale produce,
     * pero un precipita no puede romper un ciclo que está FUERA de la función.
     */
    private List<Nodo> cuerpoDeFuncion() {
        int ciclosAfuera = dentroCiclo;
        dentroCiclo = 0;
        dentroFuncion++;
        List<Nodo> cuerpo = cuerpoHasta();
        dentroFuncion--;
        dentroCiclo = ciclosAfuera;
        return cuerpo;
    }

    /** retornar = "(" "produce" [ expresion ] ")" */
    private Retornar retornar() {
        Token palabra = mirar(1);
        int linea = abrir(KW_PRODUCE);
        if (dentroFuncion == 0) {
            throw error(palabra, "'produce' outside a function");
        }
        Nodo valor = es(RPAREN) ? null : expresion();
        cerrar();
        return new Retornar(valor, linea);
    }

    /**
     * def_clase = "(" "compuesto" ID { declaracion } [ constructor ] { def_funcion } ")"
     * Se aceptan en cualquier orden, pero solo un nucleo.
     */
    private DefClase defClase() {
        int linea = abrir(KW_COMPUESTO);
        String nombre = consumir(ID, "expected a class name after 'compuesto'").lexema();
        List<Declaracion> atributos = new ArrayList<>();
        DefFuncion constructor = null;
        List<DefFuncion> metodos = new ArrayList<>();

        dentroClase++;
        while (!es(RPAREN) && !es(EOF)) {
            Token palabra = mirar(1);
            if (!es(LPAREN)) {
                throw error(actual(), "only 'elemento', 'nucleo' and 'reaccion' are allowed inside 'compuesto'");
            }
            switch (palabra.tipo()) {
                case KW_ELEMENTO -> atributos.add(declaracion());
                case KW_REACCION -> metodos.add(defFuncion());
                case KW_NUCLEO -> {
                    if (constructor != null) {
                        throw error(palabra, "a 'compuesto' can only have one 'nucleo'");
                    }
                    constructor = nucleo();
                }
                default -> throw error(palabra,
                        "only 'elemento', 'nucleo' and 'reaccion' are allowed inside 'compuesto'");
            }
        }
        dentroClase--;
        cerrar();
        return new DefClase(nombre, atributos, constructor, metodos, linea);
    }

    /** constructor = "(" "nucleo" parametros { sentencia } ")"  → se guarda como una función llamada "nucleo" */
    private DefFuncion nucleo() {
        int linea = abrir(KW_NUCLEO);
        List<String> params = parametros();
        List<Nodo> cuerpo = cuerpoDeFuncion();
        cerrar();
        return new DefFuncion("nucleo", params, cuerpo, linea);
    }

    /** intentar = "(" "ensaya" { sentencia } [ captura ] [ final ] ")" */
    private Intentar intentar() {
        int linea = abrir(KW_ENSAYA);
        List<Nodo> cuerpo = cuerpoHasta(KW_NEUTRALIZA, KW_RESIDUO);

        String varError = null;
        List<Nodo> captura = null;
        if (es(LPAREN) && mirar(1).tipo() == KW_NEUTRALIZA) {
            abrir(KW_NEUTRALIZA);
            varError = consumir(ID, "expected a variable name after 'neutraliza'").lexema();
            captura = cuerpoHasta(KW_RESIDUO);
            cerrar();
        }

        List<Nodo> finalmente = null;
        if (es(LPAREN) && mirar(1).tipo() == KW_RESIDUO) {
            abrir(KW_RESIDUO);
            finalmente = cuerpoHasta();
            cerrar();
        }

        if (captura == null && finalmente == null) {
            throw error(actual(), "'ensaya' needs 'neutraliza' or 'residuo'");
        }
        cerrar();
        return new Intentar(cuerpo, varError, captura, finalmente, linea);
    }

    /** lanzar = "(" "explota" expresion ")" */
    private Lanzar lanzar() {
        int linea = abrir(KW_EXPLOTA);
        Nodo valor = expresion();
        cerrar();
        return new Lanzar(valor, linea);
    }

    /**
     * Lee sentencias hasta encontrar ")" o un "(" seguido de alguna de las palabras dadas.
     * Así sabe dónde termina el cuerpo de un valorar (antes del revalorar o el neutro)
     * o de un ensaya (antes del neutraliza o el residuo).
     */
    private List<Nodo> cuerpoHasta(TipoToken... palabrasDeCorte) {
        List<Nodo> cuerpo = new ArrayList<>();
        while (!es(RPAREN) && !es(EOF) && !empiezaCon(palabrasDeCorte)) {
            cuerpo.add(sentencia());
        }
        return cuerpo;
    }

    private boolean empiezaCon(TipoToken... palabras) {
        if (!es(LPAREN)) return false;
        TipoToken siguiente = mirar(1).tipo();
        for (TipoToken p : palabras) {
            if (siguiente == p) return true;
        }
        return false;
    }

    // =====================================================================
    //  EXPRESIONES
    // =====================================================================

    private Nodo expresion() {
        Token t = actual();
        switch (t.tipo()) {
            case INT_NUMBER:  avanzar(); return new Literal(entero(t), t.linea());
            case REAL_NUMBER: avanzar(); return new Literal(Double.parseDouble(t.lexema()), t.linea());
            case STRING:      avanzar(); return new Literal(t.lexema(), t.linea());
            case BOOL_POLAR:  avanzar(); return new Literal(true, t.linea());
            case BOOL_APOLAR: avanzar(); return new Literal(false, t.linea());
            case NULO:        avanzar(); return new Literal(null, t.linea());
            case ID, KW_MOLECULA: return accesoOVariable();
            case LBRACKET:    return lista();
            case LPAREN:      return expresionConParentesis();
            case EOF:         throw error(t, "unexpected end of file");
            case RPAREN:      throw error(t, "unexpected ')'");
            default:          throw error(t, "unexpected '" + t.lexema() + "'");
        }
    }

    /** acceso = ( ID | "molecula" ) "." ID { "." ID }   — o una variable sola */
    private Nodo accesoOVariable() {
        Token t = avanzar();
        Nodo nodo;
        if (t.tipo() == KW_MOLECULA) {
            if (dentroClase == 0) {
                throw error(t, "'molecula' outside 'compuesto'");
            }
            nodo = new Este(t.linea());
        } else {
            nodo = new Variable(t.lexema(), t.linea());
        }
        while (es(DOT)) {
            avanzar();
            String atributo = consumir(ID, "expected an attribute name after '.'").lexema();
            nodo = new Acceso(nodo, atributo, t.linea());
        }
        return nodo;
    }

    /** lista = "[" { expresion } "]" */
    private ListaLit lista() {
        Token abre = avanzar();
        List<Nodo> elementos = new ArrayList<>();
        while (!es(RBRACKET)) {
            if (es(EOF)) throw error(actual(), "expected ']'");
            elementos.add(expresion());
        }
        avanzar();
        return new ListaLit(elementos, abre.linea());
    }

    /** Todo lo que empieza con "(" y es expresión: operaciones, absorbe, capa, masa, sintetiza y llamadas. */
    private Nodo expresionConParentesis() {
        Token abre = avanzar();          // el "("
        int linea = abre.linea();
        Token op = actual();

        if (ARITMETICOS.contains(op.tipo())) {
            avanzar();
            return operacion(op, operandos(op, 1, Integer.MAX_VALUE), linea);
        }
        if (RELACIONALES.contains(op.tipo())) {
            avanzar();
            return operacion(op, operandos(op, 2, 2), linea);
        }
        switch (op.tipo()) {
            case OP_AND, OP_OR -> {
                avanzar();
                return operacion(op, operandos(op, 2, Integer.MAX_VALUE), linea);
            }
            case OP_NOT -> {
                avanzar();
                return operacion(op, operandos(op, 1, 1), linea);
            }
            case KW_ABSORBE -> {          // leer = "(" "absorbe" [ expresion ] ")"
                avanzar();
                Nodo mensaje = es(RPAREN) ? null : expresion();
                cerrar();
                return new Leer(mensaje, linea);
            }
            case KW_CAPA -> {             // indice = "(" "capa" expresion expresion ")"
                avanzar();
                Nodo lista = expresion();
                Nodo posicion = expresion();
                cerrar();
                return new Indice(lista, posicion, linea);
            }
            case KW_MASA -> {             // tamano = "(" "masa" expresion ")"
                avanzar();
                Nodo lista = expresion();
                cerrar();
                return new Tamano(lista, linea);
            }
            case KW_SINTETIZA -> {        // nuevo = "(" "sintetiza" ID { expresion } ")"
                avanzar();
                String clase = consumir(ID, "expected a class name after 'sintetiza'").lexema();
                List<Nodo> args = argumentos();
                cerrar();
                return new Nuevo(clase, args, linea);
            }
            case ID, KW_MOLECULA -> {     // llamada = "(" ( ID | acceso ) { expresion } ")"
                Nodo funcion = accesoOVariable();
                List<Nodo> args = argumentos();
                cerrar();
                return new Llamada(funcion, args, linea);
            }
            case RPAREN -> throw error(op, "empty parentheses '()'");
            case EOF -> throw error(op, "unexpected end of file");
            default -> throw error(op, "unexpected '" + op.lexema() + "' here");
        }
    }

    private Operacion operacion(Token op, List<Nodo> operandos, int linea) {
        return new Operacion(op.tipo(), operandos, linea);
    }

    /** Lee operandos hasta el ")" y revisa que la cantidad sea la correcta para el operador. */
    private List<Nodo> operandos(Token op, int minimo, int maximo) {
        List<Nodo> lista = argumentos();
        if (lista.size() < minimo || lista.size() > maximo) {
            String esperado = (minimo == maximo) ? String.valueOf(minimo)
                    : (maximo == Integer.MAX_VALUE ? "at least " + minimo : minimo + " to " + maximo);
            throw error(op, "'" + op.lexema() + "' expects " + esperado + " operand"
                    + (esperado.equals("1") ? "" : "s") + ", got " + lista.size());
        }
        cerrar();
        return lista;
    }

    /** Expresiones hasta el ")" (sin consumirlo). */
    private List<Nodo> argumentos() {
        List<Nodo> args = new ArrayList<>();
        while (!es(RPAREN)) {
            if (es(EOF)) throw error(actual(), "expected ')'");
            args.add(expresion());
        }
        return args;
    }

    private Long entero(Token t) {
        try {
            return Long.parseLong(t.lexema());
        } catch (NumberFormatException e) {
            throw error(t, "number too large '" + t.lexema() + "'");
        }
    }

    // =====================================================================
    //  AYUDAS
    // =====================================================================

    private Token actual() {
        return tokens.get(pos);
    }

    /** Ver un token adelante sin consumirlo. mirar(0) es el actual. */
    private Token mirar(int k) {
        int i = Math.min(pos + k, tokens.size() - 1); // nunca pasar del EOF
        return tokens.get(i);
    }

    private Token avanzar() {
        Token t = actual();
        if (t.tipo() != EOF) pos++;
        return t;
    }

    private boolean es(TipoToken tipo) {
        return actual().tipo() == tipo;
    }

    private Token consumir(TipoToken esperado, String mensaje) {
        if (!es(esperado)) {
            throw error(actual(), mensaje);
        }
        return avanzar();
    }

    /** Consume "(" y la palabra reservada; devuelve la línea del "(" para el nodo. */
    private int abrir(TipoToken palabra) {
        int linea = consumir(LPAREN, "expected '('").linea();
        consumir(palabra, "expected '" + palabra + "'");
        return linea;
    }

    private void cerrar() {
        consumir(RPAREN, "expected ')'");
    }

    private ErrorSintactico error(Token t, String detalle) {
        return new ErrorSintactico(t.linea(), detalle);
    }
}
