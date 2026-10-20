package clomic.lexer;

import clomic.errores.ErrorLexico;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador léxico de Clomic.
 *
 * Recorre el código carácter por carácter, cuenta las líneas y devuelve la lista
 * de tokens. Ignora espacios, comas, saltos de línea y comentarios (~ hasta el
 * final de la línea). Al final siempre agrega un token EOF.
 */
public class Lexer {

    private static final Map<String, TipoToken> RESERVADAS = new HashMap<>();

    static {
        // Palabras reservadas
        RESERVADAS.put("elemento", TipoToken.KW_ELEMENTO);
        RESERVADAS.put("enlace", TipoToken.KW_ENLACE);
        RESERVADAS.put("valorar", TipoToken.KW_VALORAR);
        RESERVADAS.put("revalorar", TipoToken.KW_REVALORAR);
        RESERVADAS.put("neutro", TipoToken.KW_NEUTRO);
        RESERVADAS.put("catalizador", TipoToken.KW_CATALIZADOR);
        RESERVADAS.put("cristaliza", TipoToken.KW_CRISTALIZA);
        RESERVADAS.put("precipita", TipoToken.KW_PRECIPITA);
        RESERVADAS.put("sublima", TipoToken.KW_SUBLIMA);
        RESERVADAS.put("absorbe", TipoToken.KW_ABSORBE);
        RESERVADAS.put("emite", TipoToken.KW_EMITE);
        RESERVADAS.put("reaccion", TipoToken.KW_REACCION);
        RESERVADAS.put("produce", TipoToken.KW_PRODUCE);
        RESERVADAS.put("compuesto", TipoToken.KW_COMPUESTO);
        RESERVADAS.put("nucleo", TipoToken.KW_NUCLEO);
        RESERVADAS.put("sintetiza", TipoToken.KW_SINTETIZA);
        RESERVADAS.put("molecula", TipoToken.KW_MOLECULA);
        RESERVADAS.put("capa", TipoToken.KW_CAPA);
        RESERVADAS.put("ensaya", TipoToken.KW_ENSAYA);
        RESERVADAS.put("neutraliza", TipoToken.KW_NEUTRALIZA);
        RESERVADAS.put("residuo", TipoToken.KW_RESIDUO);
        RESERVADAS.put("explota", TipoToken.KW_EXPLOTA);
        RESERVADAS.put("masa", TipoToken.KW_MASA);

        // Lógicos y relacionales
        RESERVADAS.put("añadir", TipoToken.OP_AND);
        RESERVADAS.put("mezcla", TipoToken.OP_OR);
        RESERVADAS.put("inerte", TipoToken.OP_NOT);
        RESERVADAS.put("excede", TipoToken.OP_MAYOR);
        RESERVADAS.put("bajo", TipoToken.OP_MENOR);
        RESERVADAS.put("isotopo", TipoToken.OP_IGUAL);

        // Booleanos y vacío
        RESERVADAS.put("polar", TipoToken.BOOL_POLAR);
        RESERVADAS.put("apolar", TipoToken.BOOL_APOLAR);
        RESERVADAS.put("vacio", TipoToken.NULO);
    }

    private final String codigo;
    private int pos;
    private int linea;

    public Lexer(String codigo) {
        this.codigo = codigo == null ? "" : codigo;
        this.pos = 0;
        this.linea = 1;
    }

    /** Devuelve todos los tokens del programa, terminando siempre con EOF. */
    public List<Token> analizar() {
        List<Token> tokens = new ArrayList<>();

        while (pos < codigo.length()) {
            char c = codigo.charAt(pos);

            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ',') {
                if (c == '\n') {
                    linea++;
                }
                pos++;
            } else if (c == '~') {
                // Comentario de una línea: se ignora hasta el salto de línea.
                while (pos < codigo.length() && codigo.charAt(pos) != '\n') {
                    pos++;
                }
            } else if (esInicioIdentificador(c)) {
                tokens.add(leerIdentificador());
            } else if (Character.isDigit(c) || (c == '-' && esDigitoSiguiente())) {
                tokens.add(leerNumero());
            } else if (c == '"') {
                tokens.add(leerString());
            } else {
                tokens.add(leerSimbolo());
            }
        }

        tokens.add(new Token(TipoToken.EOF, "", linea));
        return tokens;
    }

    private boolean esInicioIdentificador(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean esParteIdentificador(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean esDigitoSiguiente() {
        return pos + 1 < codigo.length() && Character.isDigit(codigo.charAt(pos + 1));
    }

    private boolean sigueCon(char esperado) {
        return pos < codigo.length() && codigo.charAt(pos) == esperado;
    }

    /**
     * Lee un identificador o palabra reservada. Los operadores de dos caracteres
     * (excede=, bajo=, isotopo!) se revisan después de leer la palabra base para
     * que no queden partidos en dos tokens.
     */
    private Token leerIdentificador() {
        int lineaToken = linea;
        int inicio = pos;

        while (pos < codigo.length() && esParteIdentificador(codigo.charAt(pos))) {
            pos++;
        }

        String palabra = codigo.substring(inicio, pos);

        if (palabra.equals("excede") && sigueCon('=')) {
            pos++;
            return new Token(TipoToken.OP_MAYOR_IGUAL, "excede=", lineaToken);
        }
        if (palabra.equals("bajo") && sigueCon('=')) {
            pos++;
            return new Token(TipoToken.OP_MENOR_IGUAL, "bajo=", lineaToken);
        }
        if (palabra.equals("isotopo") && sigueCon('!')) {
            pos++;
            return new Token(TipoToken.OP_DISTINTO, "isotopo!", lineaToken);
        }

        TipoToken reservada = RESERVADAS.get(palabra);
        if (reservada != null) {
            return new Token(reservada, palabra, lineaToken);
        }
        return new Token(TipoToken.ID, palabra, lineaToken);
    }

    /**
     * Lee un entero o un real. El signo menos solo forma parte del número si va
     * pegado a un dígito: -5 es un número negativo, pero (- n 1) es una resta.
     */
    private Token leerNumero() {
        int lineaToken = linea;
        int inicio = pos;

        if (codigo.charAt(pos) == '-') {
            pos++;
        }
        while (pos < codigo.length() && Character.isDigit(codigo.charAt(pos))) {
            pos++;
        }

        boolean esReal = false;
        if (pos + 1 < codigo.length()
                && codigo.charAt(pos) == '.'
                && Character.isDigit(codigo.charAt(pos + 1))) {
            esReal = true;
            pos++;
            while (pos < codigo.length() && Character.isDigit(codigo.charAt(pos))) {
                pos++;
            }
        }

        String lexema = codigo.substring(inicio, pos);
        return new Token(esReal ? TipoToken.REAL_NUMBER : TipoToken.INT_NUMBER,
                lexema, lineaToken);
    }

    /** Lee un string y guarda el lexema sin las comillas. */
    private Token leerString() {
        int lineaToken = linea;
        pos++;

        StringBuilder contenido = new StringBuilder();
        while (pos < codigo.length()) {
            char c = codigo.charAt(pos);
            if (c == '"') {
                pos++;
                return new Token(TipoToken.STRING, contenido.toString(), lineaToken);
            }
            if (c == '\n') {
                throw new ErrorLexico(lineaToken, "unterminated string");
            }
            contenido.append(c);
            pos++;
        }
        throw new ErrorLexico(lineaToken, "unterminated string");
    }

    /** Lee operadores y delimitadores de un carácter. */
    private Token leerSimbolo() {
        char c = codigo.charAt(pos);
        int lineaToken = linea;

        TipoToken tipo = switch (c) {
            case '(' -> TipoToken.LPAREN;
            case ')' -> TipoToken.RPAREN;
            case '[' -> TipoToken.LBRACKET;
            case ']' -> TipoToken.RBRACKET;
            case '.' -> TipoToken.DOT;
            case '+' -> TipoToken.OP_SUMA;
            case '-' -> TipoToken.OP_RESTA;
            case '*' -> TipoToken.OP_MULT;
            case '/' -> TipoToken.OP_DIV;
            case '%' -> TipoToken.OP_MOD;
            default -> null;
        };

        if (tipo == null) {
            throw new ErrorLexico(lineaToken, "unrecognized symbol '" + c + "'");
        }

        pos++;
        return new Token(tipo, String.valueOf(c), lineaToken);
    }

    /** Imprime los tokens con el formato [line N] TIPO lexema. */
    public static void imprimirTokens(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.tipo() == TipoToken.EOF) {
                System.out.println("[line " + token.linea() + "] EOF");
            } else {
                System.out.println("[line " + token.linea() + "] " + token.tipo()
                        + " " + token.lexema());
            }
        }
    }
}
