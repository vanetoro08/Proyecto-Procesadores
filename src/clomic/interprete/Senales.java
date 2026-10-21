package clomic.interprete;

/**
 * Señales internas para saltar desde adentro de varios bloques anidados.
 *
 * No son errores del programa Clomic: son la forma de "saltar" en Java.
 *  - precipita lanza Romper y sublima lanza Continuar; las atrapa el ciclo más cercano.
 *  - produce lanza Retorno con el valor; la atrapa llamarFuncion.
 * Como NO heredan de ErrorEjecucion, neutraliza no las atrapa (y así debe ser).
 * super(null, null, false, false) evita armar la traza de pila, que aquí no sirve y es lenta.
 */
final class Senales {
    private Senales() {}

    static final class Romper extends RuntimeException {
        Romper() { super(null, null, false, false); }
    }

    static final class Continuar extends RuntimeException {
        Continuar() { super(null, null, false, false); }
    }

    static final class Retorno extends RuntimeException {
        final Object valor;

        Retorno(Object valor) {
            super(null, null, false, false);
            this.valor = valor;
        }
    }
}
