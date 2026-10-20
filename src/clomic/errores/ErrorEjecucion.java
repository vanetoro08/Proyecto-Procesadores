package clomic.errores;

/**
 * PROVISIONAL: la persona 5 hace la versión final.
 * Que conserve este constructor, linea() y detalle(), porque el intérprete los usa.
 */
public class ErrorEjecucion extends RuntimeException {
    private final int linea;
    private final String detalle;

    public ErrorEjecucion(int linea, String detalle) {
        super("Runtime error on line " + linea + ": " + detalle);
        this.linea = linea;
        this.detalle = detalle;
    }

    public int linea() { return linea; }

    /** Solo el texto del error, p. ej. "division by zero". Es lo que recibe neutraliza. */
    public String detalle() { return detalle; }
}
