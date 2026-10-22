package clomic.errores;
/**
 * Error del analizador sintáctico: los tokens son válidos pero no siguen la gramática.
 *
 * Ejemplos:
 *     Syntax error on line 6: expected ')'
 *     Syntax error on line 3: 'precipita' outside a loop
 */
public class ErrorSintactico extends ErrorClomic {

    public ErrorSintactico(int linea, String detalle) {
        super("Syntax", linea, detalle);
    }
}