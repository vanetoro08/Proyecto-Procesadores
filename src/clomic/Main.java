package clomic;

import clomic.ast.Nodo;
import clomic.errores.ErrorEjecucion;
import clomic.errores.ErrorLexico;
import clomic.errores.ErrorSintactico;
import clomic.interprete.Interprete;
import clomic.lexer.Lexer;
import clomic.lexer.Token;
import clomic.parser.Parser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Punto de entrada: java -cp out clomic.Main programa.txt [--tokens] */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -cp out clomic.Main programa.txt [--tokens]");
            System.exit(1);
        }

        String ruta = args[0];
        boolean soloTokens = args.length > 1 && "--tokens".equals(args[1]);

        // Leer archivo UTF-8 (quita BOM si existe)
        String codigo;
        try {
            byte[] bytes = Files.readAllBytes(Path.of(ruta));
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                byte[] sinBom = new byte[bytes.length - 3];
                System.arraycopy(bytes, 3, sinBom, 0, sinBom.length);
                codigo = new String(sinBom, StandardCharsets.UTF_8);
            } else {
                codigo = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo: " + ruta);
            System.exit(1);
            return;
        }

        try {
            // Léxico
            List<Token> tokens = new Lexer(codigo).analizar();

            if (soloTokens) {
                for (Token token : tokens) {
                    if (token.tipo() == clomic.lexer.TipoToken.EOF) {
                        System.out.println("[line " + token.linea() + "] EOF");
                    } else {
                        System.out.println("[line " + token.linea() + "] " + token.tipo() + " " + token.lexema());
                    }
                }
                return;
            }

            // Parser
            List<Nodo> arbol = new Parser(tokens).parsearPrograma();

            // Intérprete
            new Interprete().ejecutarPrograma(arbol);

        } catch (ErrorLexico e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (ErrorSintactico e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (ErrorEjecucion e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}