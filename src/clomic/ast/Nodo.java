package clomic.ast;

/** Todo nodo del árbol sabe en qué línea del .txt estaba, para reportar errores. */
public interface Nodo {
    int linea();
}
