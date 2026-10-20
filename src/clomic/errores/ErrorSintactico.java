package clomic.errores;

/** Provisional: la persona 5 sube la versión final con esta misma firma. */
public class ErrorSintactico extends RuntimeException {
    private final int linea;
    private final String detalle;

    public ErrorSintactico(int linea, String detalle) {
        super("Syntax error on line " + linea + ": " + detalle);
        this.linea = linea;
        this.detalle = detalle;
    }

    public int linea() { return linea; }

    public String detalle() { return detalle; }
}
