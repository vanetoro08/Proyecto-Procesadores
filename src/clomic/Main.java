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

/**
 * Punto de entrada: java -cp out clomic.Main programa.txt [--tokens]
 *
 * Lee el .txt, hace el análisis léxico (y con --tokens imprime los tokens),
 * el análisis sintáctico y la ejecución. Todos los errores salen por System.out
 * para que no se impriman en desorden con la salida del programa.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -cp out clomic.Main programa.txt [--tokens]");
            System.exit(1);
        }

        String ruta = args[0];
        boolean verTokens = args.length > 1 && "--tokens".equals(args[1]);

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

            if (verTokens) {
                Lexer.imprimirTokens(tokens);
                System.out.println("----------");
            }

            // Parser
            List<Nodo> arbol = new Parser(tokens).parsearPrograma();

            // Intérprete
            new Interprete().ejecutarPrograma(arbol);

        } catch (ErrorLexico e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (ErrorSintactico e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (ErrorEjecucion e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (RuntimeException | StackOverflowError e) {
            // Cualquier error inesperado de Java: mensaje corto, sin la traza
            System.out.println("Runtime error: " + e);
            System.exit(1);
        }
    }
}