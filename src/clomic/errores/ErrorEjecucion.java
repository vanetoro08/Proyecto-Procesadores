package clomic.errores;

/**
 * Error que ocurre mientras el programa se ejecuta.
 *
 * Ejemplos:
 *     Runtime error on line 9: division by zero
 *     Runtime error on line 4: index out of range
 *     Runtime error on line 2: undefined variable 'y'
 *
 * El bloque (ensaya ... (neutraliza e ...)) atrapa este tipo de error, y la variable e
 * recibe solo el detalle, por ejemplo "division by zero".
 * ExcepcionClomic (la de explota) hereda de esta clase, así que se atrapa igual.
 */
public class ErrorEjecucion extends ErrorClomic {

    public ErrorEjecucion(int linea, String detalle) {
        super("Runtime", linea, detalle);
    }
}
