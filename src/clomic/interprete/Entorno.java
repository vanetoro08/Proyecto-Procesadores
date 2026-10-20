package clomic.interprete;

import clomic.errores.ErrorEjecucion;
import java.util.HashMap;
import java.util.Map;

/**
 * Las variables visibles en un punto del programa.
 *
 * Hay un entorno global. Cada llamada a función crea uno nuevo cuyo padre es el global.
 * Buscar una variable = mirar aquí; si no está, preguntarle al padre; y así hasta el final.
 * Los ciclos y los valorar NO crean entorno nuevo (regla de alcance de la sección 7).
 */
public class Entorno {
    private final Map<String, Object> variables = new HashMap<>();
    private final Entorno padre;

    public Entorno(Entorno padre) {
        this.padre = padre;
    }

    /** elemento: crea (o vuelve a crear) la variable en ESTE entorno. */
    public void definir(String nombre, Object valor) {
        variables.put(nombre, valor);
    }

    /** enlace: cambia la variable en el entorno donde ya existe. Si no existe, es error. */
    public void asignar(String nombre, Object valor, int linea) {
        Entorno dueño = buscar(nombre);
        if (dueño == null) {
            throw new ErrorEjecucion(linea, "undefined variable '" + nombre + "'");
        }
        dueño.variables.put(nombre, valor);
    }

    /** Leer el valor de una variable. */
    public Object obtener(String nombre, int linea) {
        Entorno dueño = buscar(nombre);
        if (dueño == null) {
            throw new ErrorEjecucion(linea, "undefined variable '" + nombre + "'");
        }
        return dueño.variables.get(nombre);
    }

    /** Recorre la cadena de entornos hasta encontrar el que tiene la variable. */
    private Entorno buscar(String nombre) {
        for (Entorno e = this; e != null; e = e.padre) {
            // containsKey y no get() != null: una variable puede valer vacio (null) y aun así existir
            if (e.variables.containsKey(nombre)) {
                return e;
            }
        }
        return null;
    }
}
