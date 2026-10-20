package clomic;

import clomic.ast.Nodo;
import clomic.ast.Ast.*;
import clomic.errores.ErrorEjecucion;
import clomic.interprete.Interprete;
import clomic.lexer.TipoToken;
import java.util.List;

import static clomic.lexer.TipoToken.*;

/**
 * Prueba del intérprete SIN léxico ni parser: los árboles se arman a mano.
 * Cuando el parser esté listo, estos mismos programas se escriben en .txt.
 */
public class PruebaManual {

    // Atajos para que armar árboles no sea tan largo
    static Literal lit(Object v, int l) { return new Literal(v, l); }
    static Variable var(String n, int l) { return new Variable(n, l); }
    static Operacion op(TipoToken t, int l, Nodo... ops) { return new Operacion(t, List.of(ops), l); }
    static Mostrar emite(Nodo v, int l) { return new Mostrar(v, l); }

    public static void main(String[] args) {

        prueba("1. Ejemplo 1 del profesor (espera: Greater than 5)", List.of(
            // (elemento x 20)
            new Declaracion("x", lit(20L, 1), 1),
            // (valorar (excede x 5) (emite "...") (neutro (emite "...")))
            new Si(
                List.of(new Rama(op(OP_MAYOR, 3, var("x", 3), lit(5L, 3)),
                                 List.of(emite(lit("Greater than 5", 4), 4)))),
                List.of(emite(lit("Less than or equal to 5", 6), 6)),
                3)
        ));

        prueba("2. revalorar (espera: Cero)", List.of(
            new Declaracion("n", lit(0L, 1), 1),
            new Si(
                List.of(
                    new Rama(op(OP_MAYOR, 2, var("n", 2), lit(0L, 2)), List.of(emite(lit("Positivo", 2), 2))),
                    new Rama(op(OP_IGUAL, 3, var("n", 3), lit(0L, 3)), List.of(emite(lit("Cero", 3), 3)))),
                List.of(emite(lit("Negativo", 4), 4)),
                2)
        ));

        prueba("3. catalizador con enlace (espera: 3 2 1)", List.of(
            // (elemento n 3)
            new Declaracion("n", lit(3L, 1), 1),
            // (catalizador (excede n 0) (emite n) (enlace n (- n 1)))
            new Mientras(op(OP_MAYOR, 2, var("n", 2), lit(0L, 2)), List.of(
                emite(var("n", 3), 3),
                new Asignacion(var("n", 4), op(OP_RESTA, 4, var("n", 4), lit(1L, 4)), 4)
            ), 2)
        ));

        prueba("4. cristaliza con sublima y precipita (espera: i = 1, 3, 5, 7)", List.of(
            // (cristaliza [i 1 10]
            //   (valorar (isotopo (% i 2) 0) (sublima))
            //   (valorar (excede i 7) (precipita))
            //   (emite (+ "i = " i)))
            new Para("i", lit(1L, 1), lit(10L, 1), List.of(
                new Si(List.of(new Rama(op(OP_IGUAL, 2, op(OP_MOD, 2, var("i", 2), lit(2L, 2)), lit(0L, 2)),
                                        List.of(new Continuar(2)))), null, 2),
                new Si(List.of(new Rama(op(OP_MAYOR, 3, var("i", 3), lit(7L, 3)),
                                        List.of(new Romper(3)))), null, 3),
                emite(op(OP_SUMA, 4, lit("i = ", 4), var("i", 4)), 4)
            ), 1)
        ));

        prueba("5. Aritmética, concatenación y lógicos (espera: 3.5 / 14 / Total: 10 / polar / apolar)", List.of(
            emite(op(OP_DIV, 1, lit(7L, 1), lit(2L, 1)), 1),
            emite(op(OP_SUMA, 2, lit(2L, 2), op(OP_MULT, 2, lit(3L, 2), lit(4L, 2))), 2),
            emite(op(OP_SUMA, 3, lit("Total: ", 3), lit(10L, 3)), 3),
            emite(op(OP_AND, 4, lit(true, 4), op(OP_NOT, 4, lit(false, 4))), 4),
            emite(op(OP_OR, 5, lit(false, 5), lit(null, 5)), 5)
        ));

        prueba("6. Error: variable no declarada (espera: Runtime error on line 2)", List.of(
            new Declaracion("x", lit(1L, 1), 1),
            emite(op(OP_SUMA, 2, var("x", 2), var("y", 2)), 2)
        ));

        prueba("7. Error: división por cero (espera: Runtime error on line 1)", List.of(
            emite(op(OP_DIV, 1, lit(10L, 1), lit(0L, 1)), 1)
        ));
    }

    static void prueba(String titulo, List<Nodo> programa) {
        System.out.println("--- " + titulo);
        try {
            new Interprete().ejecutarPrograma(programa);
        } catch (ErrorEjecucion e) {
            System.out.println(e.getMessage());
        }
    }
}
