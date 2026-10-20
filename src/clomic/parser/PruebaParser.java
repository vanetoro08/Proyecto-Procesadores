package clomic.parser;

import clomic.ast.Nodo;
import clomic.ast.Ast.*;
import clomic.errores.ErrorEjecucion;
import clomic.errores.ErrorLexico;
import clomic.errores.ErrorSintactico;
import clomic.interprete.Interprete;
import clomic.lexer.Lexer;
import java.util.List;

import static clomic.lexer.TipoToken.*;

/**
 * Pruebas del parser. Ejecutar con: java -cp out clomic.parser.PruebaParser
 *
 * 1. El ejemplo 1 en texto da EXACTAMENTE el mismo árbol que está armado a mano en PruebaManual.
 * 2. Programas completos: texto → léxico → parser → intérprete.
 * 3. Los ejemplos 2 y 3 se parsean sin errores (todavía no se ejecutan: faltan funciones y clases).
 * 4. Errores sintácticos con la línea correcta.
 */
public class PruebaParser {

    static int ok = 0, fallas = 0;

    public static void main(String[] args) {
        System.out.println("===== 1. Mismo árbol que PruebaManual =====");
        compararArbolEjemplo1();

        System.out.println("\n===== 2. De punta a punta =====");
        ejecutar("Ejemplo 1 del profesor (espera: Greater than 5)", """
                (elemento x 20)

                (valorar (excede x 5)
                  (emite "Greater than 5")
                  (neutro
                    (emite "Less than or equal to 5")))
                """);
        ejecutar("revalorar (espera: Cero)", """
                (elemento n 0)
                (valorar (excede n 0) (emite "Positivo")
                  (revalorar (isotopo n 0) (emite "Cero"))
                  (neutro (emite "Negativo")))
                """);
        ejecutar("catalizador (espera: 3 2 1)", """
                (elemento n 3)
                (catalizador (excede n 0)
                  (emite n)
                  (enlace n (- n 1)))
                """);
        ejecutar("cristaliza con sublima y precipita (espera: i = 1, 3, 5, 7)", """
                (cristaliza [i 1 10]
                  (valorar (isotopo (% i 2) 0) (sublima))
                  (valorar (excede i 7) (precipita))
                  (emite (+ "i = " i)))
                """);
        ejecutar("aritmética y lógicos (espera: 3.5 / 14 / Total: 10 / polar / apolar)", """
                (emite (/ 7 2))
                (emite (+ 2 (* 3 4)))
                (emite (+ "Total: " 10))  ~ comentario al final de la línea
                (emite (añadir polar (inerte apolar)))
                (emite (mezcla apolar vacio))
                """);
        ejecutar("error de ejecución (espera: Runtime error on line 3: division by zero)", """
                (elemento a 10)
                (elemento b 0)
                (emite (/ a b))
                """);

        System.out.println("\n===== 3. Ejemplos 2 y 3 se parsean =====");
        parsea("Ejemplo 2", """
                ~ Suma de los elementos de una lista
                (reaccion sumar [lista n]
                  (elemento total 0)
                  (cristaliza [i 0 (- n 1)]
                    (enlace total (+ total (capa lista i))))
                  (produce total))

                (elemento numeros [4 8 15 16])
                (elemento resultado (sumar numeros 4))

                (valorar (añadir (excede resultado 40) (bajo resultado 50))
                  (emite (+ "The sum is " resultado))
                  (neutro
                    (emite "Out of range")))
                """, 4);
        parsea("Ejemplo 3", """
                ~ Clases y manejo de excepciones
                (compuesto Cuenta
                  (nucleo [titular saldo]
                    (enlace molecula.titular titular)
                    (enlace molecula.saldo saldo))

                  (reaccion retirar [valor]
                    (valorar (excede valor molecula.saldo)
                      (explota "Insufficient balance"))
                    (enlace molecula.saldo (- molecula.saldo valor))
                    (produce molecula.saldo)))

                (elemento c (sintetiza Cuenta "Ana" 100))

                (ensaya
                  (emite (+ "New balance: " (c.retirar 30)))
                  (emite (+ "New balance: " (c.retirar 200)))
                  (neutraliza error
                    (emite (+ "Error: " error)))
                  (residuo
                    (emite "Operation finished")))
                """, 3);

        System.out.println("\n===== 4. Errores sintácticos =====");
        falla("(elemento x 20", "Syntax error on line 1: expected ')'");
        falla("(emite 1))", "Syntax error on line 1: unexpected ')'");
        falla("(emite 1)\n(excede 5)", "Syntax error on line 2: 'excede' expects 2 operands, got 1");
        falla("(inerte a b)", "Syntax error on line 1: 'inerte' expects 1 operand, got 2");
        falla("(produce 5)", "Syntax error on line 1: 'produce' outside a function");
        falla("(emite 1)\n\n(precipita)", "Syntax error on line 3: 'precipita' outside a loop");
        falla("(catalizador polar\n  (reaccion f [] (sublima)))", "Syntax error on line 2: 'sublima' outside a loop");
        falla("(emite molecula.saldo)", "Syntax error on line 1: 'molecula' outside 'compuesto'");
        falla("(neutro (emite 1))", "Syntax error on line 1: 'neutro' outside 'valorar'");
        falla("(valorar polar (emite 1) (neutro (emite 2)) (revalorar polar (emite 3)))",
                "Syntax error on line 1: expected ')'");
        falla("(enlace 5 3)", "Syntax error on line 1: invalid assignment target");
        falla("(ensaya (emite 1))", "Syntax error on line 1: 'ensaya' needs 'neutraliza' or 'residuo'");
        falla("(emite (elemento x 1))", "Syntax error on line 1: unexpected 'elemento' here");
        falla("(elemento 5 3)", "Syntax error on line 1: expected a variable name after 'elemento'");
        falla("(cristaliza i 0 9 (emite i))", "Syntax error on line 1: expected '[' after 'cristaliza'");
        falla("(emite ())", "Syntax error on line 1: empty parentheses '()'");
        falla("(elemento lista [1 2 3)", "Syntax error on line 1: unexpected ')'");

        System.out.println("\n===== Resultado: " + ok + " OK, " + fallas + " FALLAS =====");
    }

    // ---------------------------------------------------------------------

    static List<Nodo> parsear(String codigo) {
        return new Parser(new Lexer(codigo).analizar()).parsearPrograma();
    }

    /** El mismo árbol que PruebaManual arma a mano (records se comparan con equals). */
    static void compararArbolEjemplo1() {
        List<Nodo> esperado = List.of(
            new Declaracion("x", new Literal(20L, 1), 1),
            new Si(
                List.of(new Rama(new Operacion(OP_MAYOR, List.of(new Variable("x", 3), new Literal(5L, 3)), 3),
                                 List.of(new Mostrar(new Literal("Greater than 5", 4), 4)))),
                List.of(new Mostrar(new Literal("Less than or equal to 5", 6), 6)),
                3)
        );
        List<Nodo> obtenido = parsear("""
                (elemento x 20)

                (valorar (excede x 5)
                  (emite "Greater than 5")
                  (neutro
                    (emite "Less than or equal to 5")))
                """);
        marcar(esperado.equals(obtenido), "Ejemplo 1: árbol idéntico al de PruebaManual",
                "esperado " + esperado + "\n   obtenido " + obtenido);
    }

    static void ejecutar(String titulo, String codigo) {
        System.out.println("--- " + titulo);
        try {
            new Interprete().ejecutarPrograma(parsear(codigo));
        } catch (ErrorLexico | ErrorSintactico | ErrorEjecucion e) {
            System.out.println(e.getMessage());
        }
    }

    static void parsea(String titulo, String codigo, int sentenciasEsperadas) {
        try {
            List<Nodo> arbol = parsear(codigo);
            marcar(arbol.size() == sentenciasEsperadas, titulo + ": " + arbol.size() + " sentencias",
                    "se esperaban " + sentenciasEsperadas);
        } catch (RuntimeException e) {
            marcar(false, titulo, e.getMessage());
        }
    }

    static void falla(String codigo, String mensajeEsperado) {
        try {
            parsear(codigo);
            marcar(false, codigo.replace("\n", "\\n"), "no dio error");
        } catch (ErrorSintactico e) {
            marcar(e.getMessage().equals(mensajeEsperado), e.getMessage(),
                    "se esperaba: " + mensajeEsperado);
        }
    }

    static void marcar(boolean bien, String que, String detalle) {
        if (bien) {
            ok++;
            System.out.println("OK     " + que);
        } else {
            fallas++;
            System.out.println("FALLA  " + que + "\n       " + detalle);
        }
    }
}
