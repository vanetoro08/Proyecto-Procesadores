# Clomic

Clomic es un mini lenguaje de programación basado en Clojure, con todo su vocabulario tomado de la química. Tiene la misma sintaxis de paréntesis que Clojure, pero con palabras propias: `elemento` para declarar, `valorar` para el si, `catalizador` para el mientras, `reaccion` para las funciones, `compuesto` para las clases y así.

```
(elemento x 20)

(valorar (excede x 5)
  (emite "Greater than 5")
  (neutro
    (emite "Less than or equal to 5")))
```

El procesador está escrito en Java 21 y tiene tres fases: el **léxico** convierte el texto en tokens, el **parser** revisa la gramática y arma el árbol, y el **intérprete** recorre el árbol y ejecuta el programa.

## Estructura

```
src/clomic/
├── lexer/       Lexer, Token, TipoToken            análisis léxico
├── parser/      Parser                             análisis sintáctico
├── ast/         Ast, Nodo, ImpresorArbol           nodos del árbol
├── interprete/  Interprete, Entorno, Operaciones,  ejecución
│                Senales, ClaseClomic, ObjetoClomic
├── errores/     ErrorClomic, ErrorLexico,          errores con su línea
│                ErrorSintactico, ErrorEjecucion,
│                ExcepcionClomic
├── gui/         EditorClomic, Resaltador,          interfaz gráfica (bonus)
│                NumerosDeLinea
└── Main.java                                       programa principal

pruebas/          un .txt por requisito, con la salida esperada
pruebas/errores/  programas con errores léxicos, sintácticos y de ejecución
```

## Compilar

Se necesita Java 21 o superior.

Windows (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
```

Mac o Linux:

```
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

## Ejecutar un programa

```
java -cp out clomic.Main pruebas/13_ejemplo1.txt
```

Para ver también los tokens antes de la salida:

```
java -cp out clomic.Main pruebas/13_ejemplo1.txt --tokens
```

Los errores muestran la línea donde ocurren:

```
Lexical error on line 2: unrecognized symbol '#'
Syntax error on line 6: expected ')'
Runtime error on line 9: division by zero
```

Si en Windows las tildes salen como `?`, ejecutar antes `chcp 65001`.

## Interfaz gráfica (bonus)

Un editor donde se escribe o se abre un programa, se ejecuta y se ven en pestañas la salida, los tokens, el árbol y los errores. La línea con error queda marcada en rojo y `absorbe` abre un cuadro para escribir el valor.

```
java -cp out clomic.gui.EditorClomic
java -cp out clomic.gui.EditorClomic pruebas/15_ejemplo3.txt
```

Atajos: F5 o Ctrl+Enter ejecuta, Ctrl+O abre y Ctrl+S guarda. El selector de arriba carga cualquier archivo de `pruebas/`; para que aparezcan, hay que abrir la interfaz desde la raíz del repositorio.

## Pruebas

Cada archivo de `pruebas/` cubre un requisito del enunciado y empieza con su salida esperada en comentarios (`~ Salida esperada:`). Para correrlos todos en PowerShell:

```
Get-ChildItem pruebas\*.txt, pruebas\errores\*.txt | ForEach-Object { "=== $($_.Name)"; java -cp out clomic.Main $_.FullName }
```

`07_lectura.txt` pide datos por teclado, así que se prueba a mano (escribiendo 25, Ana y 3.5).

Además hay dos clases de prueba en Java para casos que los .txt no alcanzan a cubrir:

```
java -cp out clomic.parser.PruebaParser                 17 errores sintácticos y el árbol del ejemplo 1
java -cp out clomic.interprete.PruebaOperaciones        100 casos de aritmética, comparaciones y conversiones
```