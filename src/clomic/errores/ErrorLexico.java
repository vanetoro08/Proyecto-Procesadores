package clomic.errores;
/**
 * Error del analizador léxico: un carácter que no forma ningún token válido.
 *
 * Ejemplos:
 *     Lexical error on line 2: unrecognized symbol '#'
 *     Lexical error on line 5: unterminated string
 */
public class ErrorLexico extends ErrorClomic {

    public ErrorLexico(int linea, String detalle) {
        super("Lexical", linea, detalle);
    }
}