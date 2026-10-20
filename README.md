# Clomic

Mini lenguaje basado en Clojure con vocabulario de química. Intérprete escrito en Java 21.

## Estructura

```
src/clomic/
├── lexer/       TipoToken, Token, Lexer            (persona 3)
├── parser/      Parser                             (persona 2)
├── ast/         Nodo, Ast (contrato), ImpresorArbol (persona 5)
├── interprete/  Interprete, Entorno, Senales       (persona 1)
│                Operaciones                        (persona 4)
├── errores/     ErrorLexico, ErrorSintactico,
│                ErrorEjecucion, ExcepcionClomic    (persona 5)
├── Main.java                                       (persona 3)
└── PruebaManual.java   pruebas del intérprete con árboles armados a mano
```

`Ast.java` y `TipoToken.java` son el contrato entre todos: no se cambian sin avisarle al grupo.

## Compilar

Mac o Linux:

```
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Windows (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
```

## Probar

Intérprete, sin léxico ni parser (7 pruebas, cada una dice qué salida espera):

```
java -cp out clomic.PruebaManual
```

Cuando esté el `Main`, un programa completo:

```
java -cp out clomic.Main programa.txt
java -cp out clomic.Main programa.txt --tokens
```

Si en Windows las tildes salen como `?`, ejecutar antes `chcp 65001`.

## Reglas del repositorio

- `git pull` antes de empezar.
- Solo subir código que compile.
- Cada uno trabaja en sus archivos.
- `out/` va en el `.gitignore`.
