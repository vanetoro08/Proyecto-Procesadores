package clomic.errores;

/**
 * Base de todos los errores que Clomic le muestra al usuario.
 *
 * Cada error guarda la línea del .txt donde ocurrió y el detalle (qué pasó).
 * El mensaje completo sigue el formato del profesor:
 *
 *     Lexical error on line 2: unrecognized symbol '#'
 *     Syntax error on line 6: expected ')'
 *     Runtime error on line 9: division by zero
 *
 * Las subclases solo dicen qué tipo de error son ("Lexical", "Syntax" o "Runtime").
 * Hereda de RuntimeException para no tener que declararlo con throws en cada método.
 */
public abstract class ErrorClomic extends RuntimeException {

    private final int linea;
    private final String detalle;

    protected ErrorClomic(String tipo, int linea, String detalle) {
        super(tipo + " error on line " + linea + ": " + detalle);
        this.linea = linea;
        this.detalle = detalle;
    }

    /** Línea del archivo donde ocurrió el error. */
    public int linea() {
        return linea;
    }

    /** Solo lo que pasó, sin "... error on line N:". Es lo que recibe neutraliza. */
    public String detalle() {
        return detalle;
    }
}
