package clomic.gui;

import clomic.ast.ImpresorArbol;
import clomic.ast.Nodo;
import clomic.errores.ErrorClomic;
import clomic.errores.ErrorLexico;
import clomic.errores.ErrorSintactico;
import clomic.interprete.Interprete;
import clomic.lexer.Lexer;
import clomic.lexer.TipoToken;
import clomic.lexer.Token;
import clomic.parser.Parser;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 * Interfaz gráfica de Clomic (bonus): un editor donde se escribe o se abre un programa,
 * se ejecuta y se ven en paneles separados la salida, los tokens, el árbol y los errores.
 *
 * No cambia nada del procesador: usa el mismo Lexer, Parser e Interprete que el Main.
 * Lo único especial es que la salida de emite y la entrada de absorbe se redirigen
 * a la ventana mientras corre el programa.
 *
 * Ejecutar con: java -cp out clomic.gui.EditorClomic [archivo.txt]
 */
public class EditorClomic extends JFrame {

    // ---------- Colores ----------
    private static final Color ACENTO = new Color(0x1F6F5C);
    private static final Color ACENTO_OSCURO = new Color(0x17594A);
    private static final Color FONDO = new Color(0xF6F8FA);
    private static final Color BORDE = new Color(0xD0D7DE);
    private static final Color TEXTO_SUAVE = new Color(0x57606A);
    private static final Color ROJO = new Color(0xCF222E);
    private static final Color ROJO_FONDO = new Color(0xFFEBE9);
    private static final Color VERDE = new Color(0x1A7F37);

    private static final String EJEMPLO_INICIAL = """
            ~ Bienvenida a Clomic: escribe tu programa y presiona Ejecutar (F5)
            (compuesto Cuenta
              (nucleo [titular saldo]
                (enlace molecula.titular titular)
                (enlace molecula.saldo saldo))

              (reaccion retirar [valor]
                (valorar (excede valor molecula.saldo)
                  (explota "Insufficient balance"))
                (enlace molecula.saldo (- molecula.saldo valor))
                (produce molecula.saldo)))

            (elemento c (sintetiza Cuenta "Ana" 100))

            (ensaya
              (emite (+ "New balance: " (c.retirar 30)))
              (emite (+ "New balance: " (c.retirar 200)))
              (neutraliza error
                (emite (+ "Error: " error)))
              (residuo
                (emite "Operation finished")))
            """;

    // ---------- Componentes ----------
    private final JTextPane editor = new JTextPane();
    private final NumerosDeLinea numeros;
    private final JTextPane salida = new JTextPane();
    private final DefaultTableModel modeloTokens = new DefaultTableModel(new Object[]{"Línea", "Tipo", "Lexema"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tablaTokens = new JTable(modeloTokens);
    private final JTree arbol = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Programa")));
    private final JTextArea errores = new JTextArea();
    private final JTabbedPane pestanas = new JTabbedPane();
    private final JLabel estado = new JLabel(" ");
    private final JLabel archivoActual = new JLabel("sin título");
    private final JButton botonEjecutar = new JButton("▶  Ejecutar");
    private final Font fuenteCodigo;
    private Path ruta;
    private Object marcaError;

    public EditorClomic(Path abrir) {
        super("Clomic — editor");
        fuenteCodigo = new Font(primeraFuente("Cascadia Mono", "Consolas", "JetBrains Mono",
                "DejaVu Sans Mono", Font.MONOSPACED), Font.PLAIN, 14);
        numeros = new NumerosDeLinea(editor);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(barraSuperior(), BorderLayout.NORTH);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEditor(), panelResultados());
        division.setResizeWeight(0.55);
        division.setDividerSize(6);
        division.setBorder(null);
        raiz.add(division, BorderLayout.CENTER);
        raiz.add(barraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);
        atajos();

        new Resaltador(editor);
        if (abrir != null) {
            abrirArchivo(abrir);
        } else {
            editor.setText(EJEMPLO_INICIAL);
            editor.setCaretPosition(0);
        }
        SwingUtilities.invokeLater(() -> division.setDividerLocation(0.55));
    }

    // =====================================================================
    //  CONSTRUCCIÓN DE LA VENTANA
    // =====================================================================

    private JComponent barraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Color.WHITE);
        barra.setBorder(new MatteBorder(0, 0, 1, 0, BORDE));

        JLabel logo = new JLabel("  ⚗  Clomic");
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 17f));
        logo.setForeground(ACENTO);
        logo.setBorder(new EmptyBorder(0, 6, 0, 18));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        izquierda.setOpaque(false);
        izquierda.add(logo);
        izquierda.add(botonPlano("Nuevo", e -> nuevo()));
        izquierda.add(botonPlano("Abrir…", e -> abrir()));
        izquierda.add(botonPlano("Guardar", e -> guardar()));
        izquierda.add(Box.createHorizontalStrut(10));
        izquierda.add(selectorEjemplos());

        botonEjecutar.setFont(botonEjecutar.getFont().deriveFont(Font.BOLD, 13.5f));
        botonEjecutar.setBackground(ACENTO);
        botonEjecutar.setForeground(Color.WHITE);
        botonEjecutar.setFocusPainted(false);
        botonEjecutar.setBorder(new EmptyBorder(8, 18, 8, 18));
        botonEjecutar.setOpaque(true);
        botonEjecutar.setBorderPainted(false);
        botonEjecutar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botonEjecutar.setToolTipText("Ejecutar el programa (F5 o Ctrl+Enter)");
        botonEjecutar.addActionListener(e -> ejecutar());
        botonEjecutar.getModel().addChangeListener(e ->
                botonEjecutar.setBackground(botonEjecutar.getModel().isRollover() ? ACENTO_OSCURO : ACENTO));

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        derecha.setOpaque(false);
        JLabel ayuda = new JLabel("F5 o Ctrl+Enter");
        ayuda.setForeground(TEXTO_SUAVE);
        derecha.add(ayuda);
        derecha.add(botonEjecutar);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JComponent selectorEjemplos() {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Abrir ejemplo de pruebas/…");
        File carpeta = new File("pruebas");
        File[] archivos = carpeta.listFiles((d, n) -> n.endsWith(".txt"));
        File[] errores = new File(carpeta, "errores").listFiles((d, n) -> n.endsWith(".txt"));
        if (archivos != null) {
            Arrays.sort(archivos);
            for (File f : archivos) combo.addItem(f.getName());
        }
        if (errores != null) {
            Arrays.sort(errores);
            for (File f : errores) combo.addItem("errores/" + f.getName());
        }
        combo.setEnabled(combo.getItemCount() > 1);
        combo.addActionListener(e -> {
            if (combo.getSelectedIndex() > 0) {
                abrirArchivo(Path.of("pruebas", (String) combo.getSelectedItem()));
                combo.setSelectedIndex(0);
            }
        });
        return combo;
    }

    private JComponent panelEditor() {
        editor.setFont(fuenteCodigo);
        editor.setMargin(new Insets(8, 10, 8, 10));
        editor.setBackground(Color.WHITE);
        editor.setCaretColor(ACENTO);
        editor.setSelectionColor(new Color(0xCDE8E1));
        // Tabulador de 2 espacios, como en los ejemplos
        editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { quitarMarcaError(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { quitarMarcaError(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });

        // JTextPane ajusta las líneas largas; este panel lo evita para que haya scroll horizontal
        JPanel sinAjuste = new JPanel(new BorderLayout());
        sinAjuste.add(editor);
        JScrollPane scroll = new JScrollPane(sinAjuste);
        scroll.setRowHeaderView(numeros);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(encabezado("Programa", archivoActual), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent panelResultados() {
        // Salida
        salida.setEditable(false);
        salida.setFont(fuenteCodigo);
        salida.setMargin(new Insets(8, 10, 8, 10));
        salida.setBackground(new Color(0xFBFBFC));

        // Tokens
        tablaTokens.setFont(fuenteCodigo.deriveFont(13f));
        tablaTokens.setRowHeight(22);
        tablaTokens.setShowVerticalLines(false);
        tablaTokens.setGridColor(new Color(0xEAEEF2));
        tablaTokens.getTableHeader().setReorderingAllowed(false);
        tablaTokens.getColumnModel().getColumn(0).setMaxWidth(70);
        tablaTokens.getColumnModel().getColumn(1).setPreferredWidth(170);
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        centrado.setForeground(TEXTO_SUAVE);
        tablaTokens.getColumnModel().getColumn(0).setCellRenderer(centrado);
        DefaultTableCellRenderer tipo = new DefaultTableCellRenderer();
        tipo.setForeground(ACENTO);
        tablaTokens.getColumnModel().getColumn(1).setCellRenderer(tipo);

        // Árbol
        arbol.setFont(fuenteCodigo.deriveFont(13f));
        arbol.setRowHeight(22);
        DefaultTreeCellRenderer render = new DefaultTreeCellRenderer();
        render.setLeafIcon(null);
        render.setOpenIcon(null);
        render.setClosedIcon(null);
        arbol.setCellRenderer(render);
        arbol.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Errores
        errores.setEditable(false);
        errores.setFont(fuenteCodigo);
        errores.setForeground(ROJO);
        errores.setLineWrap(true);
        errores.setWrapStyleWord(true);
        errores.setMargin(new Insets(10, 12, 10, 12));

        pestanas.addTab("Salida", sinBorde(new JScrollPane(salida)));
        pestanas.addTab("Tokens", sinBorde(new JScrollPane(tablaTokens)));
        pestanas.addTab("Árbol", sinBorde(new JScrollPane(arbol)));
        pestanas.addTab("Errores", sinBorde(new JScrollPane(errores)));
        pestanas.setFont(pestanas.getFont().deriveFont(13f));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(pestanas, BorderLayout.CENTER);
        panel.setBorder(new MatteBorder(0, 1, 0, 0, BORDE));
        return panel;
    }

    private JComponent barraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(FONDO);
        barra.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, BORDE), new EmptyBorder(5, 12, 5, 12)));
        estado.setForeground(TEXTO_SUAVE);
        estado.setText("Listo. Escribe un programa y presiona Ejecutar.");
        JLabel version = new JLabel("Clomic · léxico → parser → intérprete");
        version.setForeground(TEXTO_SUAVE);
        barra.add(estado, BorderLayout.WEST);
        barra.add(version, BorderLayout.EAST);
        return barra;
    }

    private JComponent encabezado(String titulo, JLabel detalle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(FONDO);
        p.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, BORDE), new EmptyBorder(6, 12, 6, 12)));
        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        detalle.setForeground(TEXTO_SUAVE);
        p.add(t, BorderLayout.WEST);
        p.add(detalle, BorderLayout.EAST);
        return p;
    }

    private static JButton botonPlano(String texto, java.awt.event.ActionListener accion) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.addActionListener(accion);
        return b;
    }

    private static JComponent sinBorde(JScrollPane s) {
        s.setBorder(null);
        return s;
    }

    private void atajos() {
        JRootPane rp = getRootPane();
        registrar(rp, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "ejecutar", this::ejecutar);
        registrar(rp, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "ejecutar2", this::ejecutar);
        registrar(rp, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "abrir", this::abrir);
        registrar(rp, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "guardar", this::guardar);
    }

    private static void registrar(JRootPane rp, KeyStroke tecla, String nombre, Runnable accion) {
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tecla, nombre);
        rp.getActionMap().put(nombre, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { accion.run(); }
        });
    }

    // =====================================================================
    //  ARCHIVOS
    // =====================================================================

    private void nuevo() {
        editor.setText("");
        ruta = null;
        archivoActual.setText("sin título");
        limpiarResultados();
    }

    private void abrir() {
        JFileChooser fc = new JFileChooser(ruta != null ? ruta.getParent().toFile()
                : (new File("pruebas").isDirectory() ? new File("pruebas") : new File(".")));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Programas Clomic (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            abrirArchivo(fc.getSelectedFile().toPath());
        }
    }

    private void abrirArchivo(Path p) {
        try {
            String texto = Files.readString(p, StandardCharsets.UTF_8);
            if (texto.startsWith("﻿")) texto = texto.substring(1);  // BOM del Bloc de notas
            editor.setText(texto.replace("\r\n", "\n"));
            editor.setCaretPosition(0);
            ruta = p;
            archivoActual.setText(p.getFileName().toString());
            limpiarResultados();
            estado.setText("Abierto: " + p);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo:\n" + p, "Clomic", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        if (ruta == null) {
            JFileChooser fc = new JFileChooser(new File("."));
            fc.setSelectedFile(new File("programa.txt"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            ruta = fc.getSelectedFile().toPath();
        }
        try {
            Files.writeString(ruta, editor.getText(), StandardCharsets.UTF_8);
            archivoActual.setText(ruta.getFileName().toString());
            estado.setText("Guardado: " + ruta);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + ruta, "Clomic", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    //  EJECUCIÓN
    // =====================================================================

    /**
     * Las mismas tres fases del Main, en un hilo aparte para que la ventana no se congele:
     * 1. Lexer → tabla de tokens.  2. Parser → árbol.  3. Interprete → salida.
     * Si alguna fase falla, se muestra el error y se marca la línea en el editor.
     */
    void ejecutar() {
        if (!botonEjecutar.isEnabled()) return;
        limpiarResultados();
        String codigo = editor.getText();
        botonEjecutar.setEnabled(false);
        estado.setForeground(TEXTO_SUAVE);
        estado.setText("Ejecutando…");
        pestanas.setSelectedIndex(0);

        Thread hilo = new Thread(() -> {
            long inicio = System.nanoTime();
            PrintStream outOriginal = System.out;
            InputStream inOriginal = System.in;
            SalidaVentana out = new SalidaVentana();
            String fase = "léxico";
            try {
                List<Token> tokens = new Lexer(codigo).analizar();
                SwingUtilities.invokeLater(() -> mostrarTokens(tokens));

                fase = "parser";
                List<Nodo> programa = new Parser(tokens).parsearPrograma();
                String textoArbol = ImpresorArbol.imprimir(programa);
                SwingUtilities.invokeLater(() -> mostrarArbol(textoArbol));

                fase = "ejecución";
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
                System.setIn(new EntradaVentana(out));
                new Interprete().ejecutarPrograma(programa);   // se crea DESPUÉS de cambiar System.in
                System.out.flush();

                long ms = (System.nanoTime() - inicio) / 1_000_000;
                SwingUtilities.invokeLater(() -> terminar(true, "✓ Ejecutado sin errores en " + ms + " ms", -1));
            } catch (ErrorClomic e) {
                System.out.flush();
                String tipo = e instanceof ErrorLexico ? "léxico" : e instanceof ErrorSintactico ? "sintáctico" : "de ejecución";
                SwingUtilities.invokeLater(() -> mostrarError(e.getMessage(), "Error " + tipo, e.linea()));
            } catch (StackOverflowError | RuntimeException e) {
                String f = fase;
                SwingUtilities.invokeLater(() -> mostrarError("Runtime error: " + e, "Error inesperado en " + f, -1));
            } finally {
                System.setOut(outOriginal);
                System.setIn(inOriginal);
            }
        }, "programa-clomic");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarTokens(List<Token> tokens) {
        for (Token t : tokens) {
            String lexema = t.tipo() == TipoToken.STRING ? "\"" + t.lexema() + "\"" : t.lexema();
            modeloTokens.addRow(new Object[]{t.linea(), t.tipo().name(), lexema});
        }
        pestanas.setTitleAt(1, "Tokens (" + tokens.size() + ")");
    }

    /** Convierte el texto del ImpresorArbol (sangría de 2 espacios) en un JTree. */
    private void mostrarArbol(String texto) {
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("Programa");
        Deque<DefaultMutableTreeNode> pila = new ArrayDeque<>();
        Deque<Integer> niveles = new ArrayDeque<>();
        pila.push(raiz);
        niveles.push(-1);
        for (String linea : texto.split("\n")) {
            if (linea.isBlank()) continue;
            int nivel = (linea.length() - linea.stripLeading().length()) / 2;
            while (niveles.peek() >= nivel) {
                pila.pop();
                niveles.pop();
            }
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(linea.strip());
            pila.peek().add(nodo);
            pila.push(nodo);
            niveles.push(nivel);
        }
        arbol.setModel(new DefaultTreeModel(raiz));
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);
    }

    private void mostrarError(String mensaje, String titulo, int linea) {
        agregarSalida(mensaje + "\n", ROJO, true);
        errores.setText(titulo + "\n\n" + mensaje
                + (linea > 0 ? "\n\nLa línea " + linea + " está marcada en el editor." : ""));
        pestanas.setTitleAt(3, "Errores (1)");
        pestanas.setForegroundAt(3, ROJO);
        if (linea > 0) marcarLinea(linea);
        terminar(false, "✗ " + mensaje, linea);
        pestanas.setSelectedIndex(salida.getDocument().getLength() > mensaje.length() + 1 ? 0 : 3);
    }

    private void terminar(boolean bien, String mensaje, int linea) {
        estado.setForeground(bien ? VERDE : ROJO);
        estado.setText(mensaje);
        botonEjecutar.setEnabled(true);
    }

    private void limpiarResultados() {
        salida.setText("");
        modeloTokens.setRowCount(0);
        arbol.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Programa")));
        errores.setText("Sin errores.");
        pestanas.setTitleAt(1, "Tokens");
        pestanas.setTitleAt(3, "Errores");
        pestanas.setForegroundAt(3, null);
        quitarMarcaError();
    }

    private void marcarLinea(int linea) {
        Element e = editor.getDocument().getDefaultRootElement();
        if (linea < 1 || linea > e.getElementCount()) return;
        Element fila = e.getElement(linea - 1);
        try {
            marcaError = editor.getHighlighter().addHighlight(fila.getStartOffset(), fila.getEndOffset(),
                    new DefaultHighlighter.DefaultHighlightPainter(ROJO_FONDO));
            editor.setCaretPosition(fila.getStartOffset());
        } catch (BadLocationException ignorado) { }
        numeros.marcarError(linea);
    }

    private void quitarMarcaError() {
        if (marcaError != null) {
            editor.getHighlighter().removeHighlight(marcaError);
            marcaError = null;
        }
        numeros.marcarError(-1);
    }

    private void agregarSalida(String texto, Color color, boolean negrita) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, negrita);
        try {
            StyledDocument d = salida.getStyledDocument();
            d.insertString(d.getLength(), texto, a);
            salida.setCaretPosition(d.getLength());
        } catch (BadLocationException ignorado) { }
    }

    // =====================================================================
    //  SALIDA Y ENTRADA DEL PROGRAMA
    // =====================================================================

    /** Lo que el programa imprime con emite llega aquí y se agrega al panel Salida. */
    private final class SalidaVentana extends OutputStream {
        private final ByteArrayOutputStream pendiente = new ByteArrayOutputStream();
        /** Lo impreso desde el último salto de línea: es el mensaje de absorbe. */
        private final StringBuilder lineaActual = new StringBuilder();

        @Override public synchronized void write(int b) { pendiente.write(b); }

        @Override public synchronized void write(byte[] b, int off, int len) { pendiente.write(b, off, len); }

        @Override public synchronized void flush() {
            if (pendiente.size() == 0) return;
            String texto = pendiente.toString(StandardCharsets.UTF_8);
            pendiente.reset();
            int salto = texto.lastIndexOf('\n');
            if (salto >= 0) lineaActual.setLength(0);
            lineaActual.append(salto >= 0 ? texto.substring(salto + 1) : texto);
            SwingUtilities.invokeLater(() -> agregarSalida(texto, Resaltador.TEXTO, false));
        }

        synchronized String tomarLineaActual() {
            flush();
            String t = lineaActual.toString();
            lineaActual.setLength(0);
            return t;
        }
    }

    /**
     * Lo que absorbe lee: cuando el programa pide una línea, se abre un cuadro de diálogo
     * con el mensaje que imprimió el programa, y lo escrito se muestra en la salida
     * como si se hubiera tecleado en la consola.
     */
    private final class EntradaVentana extends InputStream {
        private final SalidaVentana out;
        private byte[] buffer = new byte[0];
        private int pos = 0;
        private boolean cancelado = false;

        EntradaVentana(SalidaVentana out) { this.out = out; }

        private boolean llenar() {
            if (pos < buffer.length) return true;
            if (cancelado) return false;
            String mensaje = out.tomarLineaActual();
            AtomicReference<String> respuesta = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> respuesta.set((String) JOptionPane.showInputDialog(
                        EditorClomic.this,
                        mensaje.isBlank() ? "El programa espera un valor:" : mensaje,
                        "absorbe", JOptionPane.QUESTION_MESSAGE, null, null, "")));
            } catch (Exception e) {
                return false;
            }
            if (respuesta.get() == null) {      // Cancelar: se comporta como fin de la entrada
                cancelado = true;
                return false;
            }
            String texto = respuesta.get();
            SwingUtilities.invokeLater(() -> agregarSalida(texto + "\n", Resaltador.NUMERO, true));
            buffer = (texto + "\n").getBytes(StandardCharsets.UTF_8);
            pos = 0;
            return true;
        }

        @Override public int read() {
            return llenar() ? (buffer[pos++] & 0xFF) : -1;
        }

        /** Entrega solo lo que ya hay, para no abrir otro cuadro de diálogo de más. */
        @Override public int read(byte[] b, int off, int len) {
            if (len == 0) return 0;
            if (!llenar()) return -1;
            int n = Math.min(len, buffer.length - pos);
            System.arraycopy(buffer, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override public int available() { return buffer.length - pos; }
    }

    // =====================================================================

    private static String primeraFuente(String... nombres) {
        List<String> disponibles = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : nombres) {
            if (disponibles.contains(n)) return n;
        }
        return Font.MONOSPACED;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) { }
        Path abrir = args.length > 0 ? Path.of(args[0]) : null;
        SwingUtilities.invokeLater(() -> {
            EditorClomic ventana = new EditorClomic(abrir);
            ventana.setVisible(true);
            if (Boolean.getBoolean("clomic.ejecutarAlAbrir")) {
                ventana.ejecutar();
            }
        });
    }
}
