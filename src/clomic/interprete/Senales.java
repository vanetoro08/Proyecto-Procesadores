package clomic.interprete;

/**
 * Señales internas para salir de un ciclo desde cualquier profundidad.
 *
 * No son errores del programa Clomic: son la forma de "saltar" en Java.
 * precipita lanza Romper, sublima lanza Continuar, y el ciclo que las atrapa decide qué hacer.
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
}
