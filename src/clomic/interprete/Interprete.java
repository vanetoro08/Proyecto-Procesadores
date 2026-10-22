package clomic.interprete;

import clomic.ast.Nodo;
import clomic.ast.Ast.*;
import clomic.errores.ErrorEjecucion;
import clomic.errores.ExcepcionClomic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * Recorre el árbol y lo ejecuta.
 *
 * Dos métodos hacen casi todo:
 *  - ejecutar(nodo): para sentencias, que HACEN algo (declarar, mostrar, repetir...).
 *  - evaluar(nodo):  para expresiones, que DEVUELVEN un valor (20, x, (+ x 1)...).
 *
 * Partes:
 *  - Persona 1: variables, expresiones, control de flujo, funciones y excepciones.
 *  - Persona 2: listas y clases, al final del archivo.
 */
public class Interprete {

    private final Entorno global = new Entorno(null);
    private final BufferedReader teclado =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    /** Nombre interno con el que se guarda el objeto actual. No choca con nada: molecula es palabra reservada. */
    static final String MOLECULA = "molecula";

    /** Punto de entrada: ejecuta el programa completo en el entorno global. */
    public void ejecutarPrograma(List<Nodo> programa) {
        ejecutarBloque(programa, global);
    }

    // =====================================================================
    //  SENTENCIAS
    // =====================================================================

    void ejecutarBloque(List<Nodo> sentencias, Entorno entorno) {
        for (Nodo sentencia : sentencias) {
            ejecutar(sentencia, entorno);
        }
    }

    void ejecutar(Nodo nodo, Entorno entorno) {
        switch (nodo) {
            case Declaracion d -> {
                Object valor = (d.valor() == null) ? null : evaluar(d.valor(), entorno);
                entorno.definir(d.nombre(), valor);
            }
            case Asignacion a -> asignar(a, entorno);
            case Mostrar m -> System.out.println(Operaciones.aTexto(evaluar(m.valor(), entorno)));
            case Si s -> ejecutarSi(s, entorno);
            case Mientras m -> ejecutarMientras(m, entorno);
            case Para p -> ejecutarPara(p, entorno);
            case Romper r -> throw new Senales.Romper();
            case Continuar c -> throw new Senales.Continuar();
            // Funciones: definir una función es guardarla en el entorno con su nombre
            case DefFuncion f -> entorno.definir(f.nombre(), f);
            case Retornar r -> {
                Object valor = (r.valor() == null) ? null : evaluar(r.valor(), entorno);
                throw new Senales.Retorno(valor);
            }
            // Excepciones
            case Intentar t -> ejecutarIntentar(t, entorno);
            case Lanzar l -> {
                Object valor = evaluar(l.valor(), entorno);
                throw new ExcepcionClomic(Operaciones.aTexto(valor), l.linea());
            }
            // Clases
            case DefClase d -> definirClase(d, entorno);
            // Una expresión suelta como sentencia, por ejemplo una llamada (saludar "Ana")
            default -> evaluar(nodo, entorno);
        }
    }

    private void asignar(Asignacion a, Entorno entorno) {
        Object valor = evaluar(a.valor(), entorno);
        switch (a.destino()) {
            case Variable v -> entorno.asignar(v.nombre(), valor, a.linea());
            case Indice i -> asignarIndice(i, valor, entorno);
            case Acceso ac -> asignarAtributo(ac, valor, entorno);
            default -> throw new ErrorEjecucion(a.linea(), "invalid assignment target");
        }
    }

    /** valorar / revalorar / neutro: la primera rama verdadera gana; si ninguna, el neutro. */
    private void ejecutarSi(Si s, Entorno entorno) {
        for (Rama rama : s.ramas()) {
            if (esVerdadero(evaluar(rama.condicion(), entorno))) {
                ejecutarBloque(rama.cuerpo(), entorno);
                return;
            }
        }
        if (s.sino() != null) {
            ejecutarBloque(s.sino(), entorno);
        }
    }

    /** catalizador = while. */
    private void ejecutarMientras(Mientras m, Entorno entorno) {
        while (esVerdadero(evaluar(m.condicion(), entorno))) {
            try {
                ejecutarBloque(m.cuerpo(), entorno);
            } catch (Senales.Continuar c) {
                // sublima: se deja el resto del cuerpo y se vuelve a revisar la condición
            } catch (Senales.Romper r) {
                break; // precipita: se sale del ciclo
            }
        }
    }

    /** cristaliza [i inicio fin]: el fin es inclusivo, así que [i 0 9] da 0, 1, ..., 9. */
    private void ejecutarPara(Para p, Entorno entorno) {
        long inicio = Operaciones.entero(evaluar(p.inicio(), entorno), p.linea());
        long fin = Operaciones.entero(evaluar(p.fin(), entorno), p.linea());
        for (long i = inicio; i <= fin; i++) {
            entorno.definir(p.variable(), i);
            try {
                ejecutarBloque(p.cuerpo(), entorno);
            } catch (Senales.Continuar c) {
                // sublima: pasa al siguiente i
            } catch (Senales.Romper r) {
                break;
            }
        }
    }

    // =====================================================================
    //  FUNCIONES
    // =====================================================================

    /**
     * Llama una función (o un método o el constructor, que también son DefFuncion).
     *
     * 1. Revisa que lleguen tantos argumentos como parámetros.
     * 2. Crea un entorno NUEVO cuyo padre es el global: las variables de la función son locales
     *    y desaparecen al terminar, pero desde adentro se ven las globales y las otras funciones.
     * 3. Si es un método, guarda el objeto como "molecula" en ese entorno.
     * 4. Ejecuta el cuerpo. Si aparece produce, la señal Retorno trae el valor hasta aquí.
     *    Si no hay produce, la función devuelve vacio (null).
     */
    Object llamarFuncion(DefFuncion f, List<Object> args, Object este, int linea) {
        if (args.size() != f.params().size()) {
            throw new ErrorEjecucion(linea, "'" + f.nombre() + "' expects " + f.params().size()
                    + " argument" + (f.params().size() == 1 ? "" : "s") + ", got " + args.size());
        }
        Entorno local = new Entorno(global);
        for (int i = 0; i < args.size(); i++) {
            local.definir(f.params().get(i), args.get(i));
        }
        if (este != null) {
            local.definir(MOLECULA, este);
        }
        try {
            ejecutarBloque(f.cuerpo(), local);
            return null;
        } catch (Senales.Retorno r) {
            return r.valor;
        } catch (StackOverflowError e) {
            // Recursión infinita: Java se queda sin pila
            throw new ErrorEjecucion(linea, "stack overflow (infinite recursion?)");
        }
    }

    /** (nombre args...) o (objeto.metodo args...) */
    private Object evaluarLlamada(Llamada llamada, Entorno entorno) {
        List<Object> args = new ArrayList<>();
        for (Nodo a : llamada.args()) {
            args.add(evaluar(a, entorno));
        }
        switch (llamada.funcion()) {
            case Acceso metodo -> {
                return llamarMetodo(metodo, args, llamada.linea(), entorno);
            }
            case Variable v -> {
                Object valor = entorno.obtener(v.nombre(), v.linea());
                if (!(valor instanceof DefFuncion f)) {
                    throw new ErrorEjecucion(llamada.linea(), "'" + v.nombre() + "' is not a function");
                }
                return llamarFuncion(f, args, null, llamada.linea());
            }
            default -> throw new ErrorEjecucion(llamada.linea(), "this is not a function");
        }
    }

    // =====================================================================
    //  EXCEPCIONES
    // =====================================================================

    /**
     * ensaya / neutraliza / residuo = try / catch / finally.
     *
     * Se atrapa ErrorEjecucion, que incluye:
     *  - ExcepcionClomic (lo que lanza explota), porque hereda de ErrorEjecucion;
     *  - los errores del sistema: division by zero, index out of range, undefined variable...
     * La variable de neutraliza recibe solo el mensaje (detalle), no "Runtime error on line...".
     * Las señales (precipita, sublima, produce) NO son ErrorEjecucion, así que pasan derecho;
     * pero el finally de Java hace que residuo se ejecute igual.
     */
    private void ejecutarIntentar(Intentar t, Entorno entorno) {
        try {
            ejecutarBloque(t.cuerpo(), entorno);
        } catch (ErrorEjecucion error) {
            if (t.captura() == null) {
                throw error;  // sin neutraliza: el error sigue hacia afuera (después del residuo)
            }
            entorno.definir(t.varError(), error.detalle());
            ejecutarBloque(t.captura(), entorno);
        } finally {
            if (t.finalmente() != null) {
                ejecutarBloque(t.finalmente(), entorno);
            }
        }
    }

    // =====================================================================
    //  EXPRESIONES
    // =====================================================================

    Object evaluar(Nodo nodo, Entorno entorno) {
        return switch (nodo) {
            case Literal l -> l.valor();
            case Variable v -> entorno.obtener(v.nombre(), v.linea());
            case Operacion o -> evaluarOperacion(o, entorno);
            case Leer l -> leer(l, entorno);
            case Llamada ll -> evaluarLlamada(ll, entorno);
            case Este e -> entorno.obtener(MOLECULA, e.linea());
            // Listas y clases
            case ListaLit l -> crearLista(l, entorno);
            case Indice i -> leerIndice(i, entorno);
            case Tamano t -> tamano(t, entorno);
            case Nuevo n -> crearObjeto(n, entorno);
            case Acceso a -> leerAtributo(a, entorno);
            default -> throw new ErrorEjecucion(nodo.linea(),
                    "not implemented yet: " + nodo.getClass().getSimpleName());
        };
    }

    private Object evaluarOperacion(Operacion o, Entorno entorno) {
        List<Nodo> nodos = o.operandos();
        int linea = o.linea();

        // Los lógicos van primero y evalúan de a uno ("cortocircuito"):
        // en (añadir a b), si a es apolar, b ni siquiera se evalúa.
        switch (o.op()) {
            case OP_AND -> { return todasVerdaderas(nodos, entorno); }
            case OP_OR -> { return algunaVerdadera(nodos, entorno); }
            case OP_NOT -> { return !esVerdadero(evaluar(nodos.get(0), entorno)); }
            default -> { }
        }

        // El resto necesita todos los operandos ya evaluados
        List<Object> v = new ArrayList<>();
        for (Nodo n : nodos) {
            v.add(evaluar(n, entorno));
        }

        return switch (o.op()) {
            case OP_SUMA -> acumular(v, (a, b) -> Operaciones.sumar(a, b, linea));
            case OP_RESTA -> v.size() == 1
                    ? Operaciones.negar(v.get(0), linea)
                    : acumular(v, (a, b) -> Operaciones.restar(a, b, linea));
            case OP_MULT -> acumular(v, (a, b) -> Operaciones.multiplicar(a, b, linea));
            case OP_DIV -> acumular(v, (a, b) -> Operaciones.dividir(a, b, linea));
            case OP_MOD -> acumular(v, (a, b) -> Operaciones.modulo(a, b, linea));
            case OP_MAYOR -> Operaciones.comparar(v.get(0), v.get(1), linea) > 0;
            case OP_MENOR -> Operaciones.comparar(v.get(0), v.get(1), linea) < 0;
            case OP_MAYOR_IGUAL -> Operaciones.comparar(v.get(0), v.get(1), linea) >= 0;
            case OP_MENOR_IGUAL -> Operaciones.comparar(v.get(0), v.get(1), linea) <= 0;
            case OP_IGUAL -> Operaciones.sonIguales(v.get(0), v.get(1));
            case OP_DISTINTO -> !Operaciones.sonIguales(v.get(0), v.get(1));
            default -> throw new ErrorEjecucion(linea, "unknown operator " + o.op());
        };
    }

    /** (+ 1 2 3) = ((1 + 2) + 3): se aplica la operación de izquierda a derecha. */
    private Object acumular(List<Object> valores, BinaryOperator<Object> operacion) {
        Object resultado = valores.get(0);
        for (int i = 1; i < valores.size(); i++) {
            resultado = operacion.apply(resultado, valores.get(i));
        }
        return resultado;
    }

    private boolean todasVerdaderas(List<Nodo> nodos, Entorno entorno) {
        for (Nodo n : nodos) {
            if (!esVerdadero(evaluar(n, entorno))) return false;
        }
        return true;
    }

    private boolean algunaVerdadera(List<Nodo> nodos, Entorno entorno) {
        for (Nodo n : nodos) {
            if (esVerdadero(evaluar(n, entorno))) return true;
        }
        return false;
    }

    /** absorbe: muestra el mensaje (si hay), lee una línea y la convierte. */
    private Object leer(Leer l, Entorno entorno) {
        if (l.mensaje() != null) {
            System.out.print(Operaciones.aTexto(evaluar(l.mensaje(), entorno)));
            System.out.flush();
        }
        try {
            String texto = teclado.readLine();
            if (texto == null) {
                throw new ErrorEjecucion(l.linea(), "no input available");
            }
            return Operaciones.convertirEntrada(texto.trim());
        } catch (IOException e) {
            throw new ErrorEjecucion(l.linea(), "could not read input");
        }
    }

    /** Igual que en Clojure: solo apolar y vacio son falsos; todo lo demás es verdadero. */
    static boolean esVerdadero(Object valor) {
        return valor != null && !Boolean.FALSE.equals(valor);
    }

    // =====================================================================
    //  LISTAS (persona 2)
    // =====================================================================

    /** [4 8 15]: evaluar cada elemento y devolver un ArrayList nuevo (modificable, a diferencia de List.of). */
    private Object crearLista(ListaLit l, Entorno entorno) {
        List<Object> lista = new ArrayList<>();
        for (Nodo elemento : l.elementos()) {
            lista.add(evaluar(elemento, entorno));
        }
        return lista;
    }

    /** (capa lista i): índice desde 0. */
    private Object leerIndice(Indice i, Entorno entorno) {
        List<Object> lista = comoLista(evaluar(i.lista(), entorno), i.linea());
        int posicion = posicionValida(lista, evaluar(i.posicion(), entorno), i.linea());
        return lista.get(posicion);
    }

    /** (enlace (capa lista i) valor): cambia el elemento en su lugar. */
    private void asignarIndice(Indice destino, Object valor, Entorno entorno) {
        List<Object> lista = comoLista(evaluar(destino.lista(), entorno), destino.linea());
        int posicion = posicionValida(lista, evaluar(destino.posicion(), entorno), destino.linea());
        lista.set(posicion, valor);
    }

    /** (masa lista): cantidad de elementos, o de letras si es un string. */
    private Object tamano(Tamano t, Entorno entorno) {
        return Operaciones.tamano(evaluar(t.lista(), entorno), t.linea());
    }

    @SuppressWarnings("unchecked")
    private List<Object> comoLista(Object valor, int linea) {
        if (valor instanceof List<?> lista) {
            return (List<Object>) lista;
        }
        throw new ErrorEjecucion(linea, "expected a list but got " + tipoDe(valor));
    }

    /** El índice tiene que ser entero y estar entre 0 y tamaño - 1. El texto del error es el que recibe neutraliza. */
    private int posicionValida(List<Object> lista, Object indice, int linea) {
        long posicion = Operaciones.entero(indice, linea);
        if (posicion < 0 || posicion >= lista.size()) {
            throw new ErrorEjecucion(linea, "index out of range");
        }
        return (int) posicion;
    }

    // =====================================================================
    //  CLASES (persona 2)
    // =====================================================================

    /**
     * (compuesto Cuenta ...): arma la ClaseClomic y la guarda en el entorno con su nombre.
     * El constructor se guarda con el nombre de la clase para que, si llegan mal los argumentos,
     * el error diga 'Cuenta' expects 2 arguments en vez de 'nucleo' expects 2 arguments.
     */
    private void definirClase(DefClase d, Entorno entorno) {
        Map<String, DefFuncion> metodos = new LinkedHashMap<>();
        for (DefFuncion metodo : d.metodos()) {
            metodos.put(metodo.nombre(), metodo);
        }
        DefFuncion constructor = null;
        if (d.constructor() != null) {
            DefFuncion c = d.constructor();
            constructor = new DefFuncion(d.nombre(), c.params(), c.cuerpo(), c.linea());
        }
        entorno.definir(d.nombre(), new ClaseClomic(d.nombre(), d.atributos(), constructor, metodos));
    }

    /**
     * (sintetiza Cuenta "Ana" 100):
     * 1. busca la clase,
     * 2. crea el objeto vacío,
     * 3. le pone los atributos declarados con elemento (con su valor inicial o vacio),
     * 4. llama al constructor con el objeto como molecula.
     */
    private Object crearObjeto(Nuevo n, Entorno entorno) {
        Object valor = entorno.obtener(n.clase(), n.linea());
        if (!(valor instanceof ClaseClomic clase)) {
            throw new ErrorEjecucion(n.linea(), "'" + n.clase() + "' is not a class");
        }
        ObjetoClomic objeto = new ObjetoClomic(clase);
        for (Declaracion atributo : clase.atributos()) {
            Object inicial = (atributo.valor() == null) ? null : evaluar(atributo.valor(), entorno);
            objeto.poner(atributo.nombre(), inicial);
        }

        List<Object> args = new ArrayList<>();
        for (Nodo a : n.args()) {
            args.add(evaluar(a, entorno));
        }
        if (clase.constructor() != null) {
            llamarFuncion(clase.constructor(), args, objeto, n.linea());
        } else if (!args.isEmpty()) {
            throw new ErrorEjecucion(n.linea(), "'" + clase.nombre() + "' has no constructor");
        }
        return objeto;
    }

    /** c.saldo / molecula.saldo */
    private Object leerAtributo(Acceso a, Entorno entorno) {
        ObjetoClomic objeto = comoObjeto(evaluar(a.objeto(), entorno), a.linea());
        if (!objeto.tieneAtributo(a.atributo())) {
            throw new ErrorEjecucion(a.linea(),
                    "'" + objeto.clase().nombre() + "' has no attribute '" + a.atributo() + "'");
        }
        return objeto.obtener(a.atributo());
    }

    /** (enlace molecula.saldo valor): crea el atributo si no existe, como hace el constructor del ejemplo 3. */
    private void asignarAtributo(Acceso destino, Object valor, Entorno entorno) {
        ObjetoClomic objeto = comoObjeto(evaluar(destino.objeto(), entorno), destino.linea());
        objeto.poner(destino.atributo(), valor);
    }

    /** (c.retirar 30): busca el método en la clase del objeto y lo llama con el objeto como molecula. */
    private Object llamarMetodo(Acceso metodo, List<Object> args, int linea, Entorno entorno) {
        ObjetoClomic objeto = comoObjeto(evaluar(metodo.objeto(), entorno), linea);
        DefFuncion f = objeto.clase().metodos().get(metodo.atributo());
        if (f == null) {
            throw new ErrorEjecucion(linea,
                    "'" + objeto.clase().nombre() + "' has no method '" + metodo.atributo() + "'");
        }
        return llamarFuncion(f, args, objeto, linea);
    }

    private ObjetoClomic comoObjeto(Object valor, int linea) {
        if (valor instanceof ObjetoClomic objeto) {
            return objeto;
        }
        throw new ErrorEjecucion(linea, "expected an object but got " + tipoDe(valor));
    }

    /** Nombre del tipo para los mensajes de error. */
    private static String tipoDe(Object v) {
        if (v == null) return "vacio";
        if (v instanceof Long) return "integer";
        if (v instanceof Double) return "real";
        if (v instanceof String) return "string";
        if (v instanceof Boolean) return "boolean";
        if (v instanceof List) return "list";
        if (v instanceof ObjetoClomic o) return "object " + o;
        if (v instanceof ClaseClomic) return "class";
        if (v instanceof DefFuncion) return "function";
        return v.getClass().getSimpleName();
    }
}
