package clomic.errores;

/**
 * La excepción que lanza (explota "mensaje") en un programa Clomic.
 *
 * Hereda de ErrorEjecucion a propósito:
 *  - neutraliza la atrapa con el mismo mecanismo que una división por cero;
 *  - si nadie la atrapa, el Main la muestra como "Runtime error on line N: mensaje" sin cambios.
 */
public class ExcepcionClomic extends ErrorEjecucion {

    public ExcepcionClomic(String mensaje, int linea) {
        super(linea, mensaje);
    }

    /** El texto que se lanzó con explota. Es lo mismo que detalle(). */
    public String mensaje() {
        return detalle();
    }
}
