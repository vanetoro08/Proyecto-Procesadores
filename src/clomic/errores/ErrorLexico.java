package clomic.errores;

/**
 * PROVISIONAL: la persona 5 hace la versión final.
 * Que conserve este constructor, linea() y detalle(), porque el léxico los usa.
 */
public class ErrorLexico extends RuntimeException {
    private final int linea;
    private final String detalle;

    public ErrorLexico(int linea, String detalle) {
        super("Lexical error on line " + linea + ": " + detalle);
        this.linea = linea;
        this.detalle = detalle;
    }

    public int linea() { return linea; }

    public String detalle() { return detalle; }
}
