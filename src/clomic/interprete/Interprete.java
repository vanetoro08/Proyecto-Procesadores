package clomic.interprete;

import clomic.ast.Nodo;
import clomic.ast.Ast.*;
import clomic.errores.ErrorEjecucion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

/**
 * Recorre el árbol y lo ejecuta.
 *
 * Dos métodos hacen casi todo:
 *  - ejecutar(nodo): para sentencias, que HACEN algo (declarar, mostrar, repetir...).
 *  - evaluar(nodo):  para expresiones, que DEVUELVEN un valor (20, x, (+ x 1)...).
 *
 * Martes:    variables, expresiones, emite/absorbe, valorar, catalizador, cristaliza, precipita/sublima.
 * Miércoles: funciones, excepciones (y la persona 2: listas y clases).
 */
public class Interprete {

    private final Entorno global = new Entorno(null);
    private final BufferedReader teclado =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

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
            // Una expresión suelta como sentencia, por ejemplo una llamada (saludar "Ana")
            default -> evaluar(nodo, entorno);
        }
    }

    private void asignar(Asignacion a, Entorno entorno) {
        Object valor = evaluar(a.valor(), entorno);
        switch (a.destino()) {
            case Variable v -> entorno.asignar(v.nombre(), valor, a.linea());
            // Acceso (c.saldo) e Indice (capa lista 2) los agrega la persona 2 el miércoles
            default -> throw new ErrorEjecucion(a.linea(),
                    "not implemented yet: assignment to " + a.destino().getClass().getSimpleName());
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
    //  EXPRESIONES
    // =====================================================================

    Object evaluar(Nodo nodo, Entorno entorno) {
        return switch (nodo) {
            case Literal l -> l.valor();
            case Variable v -> entorno.obtener(v.nombre(), v.linea());
            case Operacion o -> evaluarOperacion(o, entorno);
            case Leer l -> leer(l, entorno);
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
}
