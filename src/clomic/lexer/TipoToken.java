package clomic.lexer;

/** Tipos de token de Clomic (contrato del documento de diseño). Lo mantiene la persona del léxico. */
public enum TipoToken {
    // Palabras reservadas
    KW_ELEMENTO, KW_ENLACE, KW_VALORAR, KW_REVALORAR, KW_NEUTRO,
    KW_CATALIZADOR, KW_CRISTALIZA, KW_PRECIPITA, KW_SUBLIMA,
    KW_ABSORBE, KW_EMITE, KW_REACCION, KW_PRODUCE,
    KW_COMPUESTO, KW_NUCLEO, KW_SINTETIZA, KW_MOLECULA, KW_CAPA,
    KW_ENSAYA, KW_NEUTRALIZA, KW_RESIDUO, KW_EXPLOTA, KW_MASA,
    // Lógicos y relacionales
    OP_AND, OP_OR, OP_NOT,
    OP_MAYOR, OP_MENOR, OP_MAYOR_IGUAL, OP_MENOR_IGUAL, OP_IGUAL, OP_DISTINTO,
    // Aritméticos
    OP_SUMA, OP_RESTA, OP_MULT, OP_DIV, OP_MOD,
    // Literales e identificadores
    INT_NUMBER, REAL_NUMBER, STRING, BOOL_POLAR, BOOL_APOLAR, NULO, ID,
    // Delimitadores
    LPAREN, RPAREN, LBRACKET, RBRACKET, DOT,
    EOF
}
