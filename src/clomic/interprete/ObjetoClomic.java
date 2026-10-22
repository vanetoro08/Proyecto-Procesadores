package clomic.interprete;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un objeto creado con (sintetiza Cuenta ...).
 *
 * Sabe de qué clase es (para buscar sus métodos) y guarda sus propios atributos.
 * Cada objeto tiene su propio mapa, así que dos cuentas no comparten el saldo.
 * LinkedHashMap mantiene el orden en que se crearon los atributos.
 */
public class ObjetoClomic {

    private final ClaseClomic clase;
    private final Map<String, Object> atributos = new LinkedHashMap<>();

    public ObjetoClomic(ClaseClomic clase) {
        this.clase = clase;
    }

    public ClaseClomic clase() {
        return clase;
    }

    public boolean tieneAtributo(String nombre) {
        return atributos.containsKey(nombre);
    }

    public Object obtener(String nombre) {
        return atributos.get(nombre);
    }

    /** Crea el atributo si no existe; si existe, cambia su valor. */
    public void poner(String nombre, Object valor) {
        atributos.put(nombre, valor);
    }

    @Override
    public String toString() {
        return "<" + clase.nombre() + ">";
    }
}
