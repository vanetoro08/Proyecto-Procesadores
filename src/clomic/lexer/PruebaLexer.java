package clomic.lexer;

import clomic.errores.ErrorLexico;
import java.util.List;

/**
 * Pruebas manuales del léxico.
 * Ejecutar con: java -cp out clomic.lexer.PruebaLexer
 */
public class PruebaLexer {

    public static void main(String[] args) {
        probar("1. Ejemplo 1 del profesor", """
                (elemento x 20)
                (valorar (excede x 5)
                  (emite "Greater than 5")
                  (neutro
                    (emite "Less than or equal to 5")))
                """);

        probar("2. Ejemplo 2 del profesor", """
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
                """);

        probar("3. Ejemplo 3 del profesor", """
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
                """);

        probar("4. Operadores de dos caracteres y números negativos",
                "(isotopo! x 5) (excede= y 3) (bajo z -2)");

        probar("5. Punto como acceso y real",
                "(emite c.saldo) (elemento pi 3.14)");

        probar("6. Error: símbolo inválido",
                "(elemento x #)");

        probar("7. Error: string sin cerrar",
                "(emite \"hola)");
    }

    private static void probar(String titulo, String codigo) {
        System.out.println("=== " + titulo + " ===");
        try {
            List<Token> tokens = new Lexer(codigo).analizar();
            Lexer.imprimirTokens(tokens);
        } catch (ErrorLexico e) {
            System.out.println(e.getMessage());
        }
        System.out.println();
    }
}
