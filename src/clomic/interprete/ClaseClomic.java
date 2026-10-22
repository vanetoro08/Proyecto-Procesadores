package clomic.interprete;

import clomic.ast.Ast.Declaracion;
import clomic.ast.Ast.DefFuncion;
import java.util.List;
import java.util.Map;

/**
 * Una clase de Clomic ya definida: lo que queda guardado en el entorno después de ejecutar
 * (compuesto Cuenta ...).
 *
 * @param nombre      nombre de la clase, por ejemplo "Cuenta"
 * @param atributos   los (elemento ...) de la clase, con su valor inicial
 * @param constructor el nucleo, o null si la clase no tiene
 * @param metodos     las reacciones de la clase, por nombre
 */
public record ClaseClomic(String nombre, List<Declaracion> atributos, DefFuncion constructor,
                          Map<String, DefFuncion> metodos) {

    @Override
    public String toString() {
        return "<compuesto " + nombre + ">";
    }
}
