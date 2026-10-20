package clomic.lexer;

/** Un token producido por el léxico: tipo, texto original y línea donde apareció. */
public record Token(TipoToken tipo, String lexema, int linea) {
}
