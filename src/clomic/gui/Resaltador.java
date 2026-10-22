package clomic.gui;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Colorea el código Clomic mientras se escribe: palabras reservadas por grupo,
 * strings, números y comentarios.
 *
 * Solo pinta; no analiza nada. Usa una expresión regular sencilla en vez del Lexer
 * porque tiene que funcionar también con código a medio escribir (con errores).
 * Para no recolorear en cada tecla, espera 150 ms después del último cambio.
 */
final class Resaltador implements DocumentListener {

    static final Color TEXTO = new Color(0x1F2328);
    static final Color CONTROL = new Color(0x8250DF);     // valorar, catalizador, cristaliza...
    static final Color DEFINICION = new Color(0x0B6E99);  // elemento, reaccion, compuesto...
    static final Color EXCEPCION = new Color(0xB35900);   // ensaya, neutraliza, residuo, explota
    static final Color OPERADOR = new Color(0x1F6F5C);    // excede, añadir, capa, masa...
    static final Color LITERAL = new Color(0xCF222E);     // polar, apolar, vacio
    static final Color STRING = new Color(0x0A7F3F);
    static final Color NUMERO = new Color(0x0550AE);
    static final Color COMENTARIO = new Color(0x8C959F);

    private static final Map<String, Color> PALABRAS = Map.ofEntries(
            Map.entry("valorar", CONTROL), Map.entry("revalorar", CONTROL), Map.entry("neutro", CONTROL),
            Map.entry("catalizador", CONTROL), Map.entry("cristaliza", CONTROL),
            Map.entry("precipita", CONTROL), Map.entry("sublima", CONTROL),
            Map.entry("elemento", DEFINICION), Map.entry("enlace", DEFINICION),
            Map.entry("reaccion", DEFINICION), Map.entry("produce", DEFINICION),
            Map.entry("compuesto", DEFINICION), Map.entry("nucleo", DEFINICION),
            Map.entry("sintetiza", DEFINICION), Map.entry("molecula", DEFINICION),
            Map.entry("emite", DEFINICION), Map.entry("absorbe", DEFINICION),
            Map.entry("ensaya", EXCEPCION), Map.entry("neutraliza", EXCEPCION),
            Map.entry("residuo", EXCEPCION), Map.entry("explota", EXCEPCION),
            Map.entry("añadir", OPERADOR), Map.entry("mezcla", OPERADOR), Map.entry("inerte", OPERADOR),
            Map.entry("excede", OPERADOR), Map.entry("excede=", OPERADOR),
            Map.entry("bajo", OPERADOR), Map.entry("bajo=", OPERADOR),
            Map.entry("isotopo", OPERADOR), Map.entry("isotopo!", OPERADOR),
            Map.entry("capa", OPERADOR), Map.entry("masa", OPERADOR),
            Map.entry("polar", LITERAL), Map.entry("apolar", LITERAL), Map.entry("vacio", LITERAL));

    /** Un solo patrón con alternativas; el orden importa: un ~ dentro de un string no es comentario. */
    private static final Pattern TOKENS = Pattern.compile(
            "(?<comentario>~[^\\n]*)"
            + "|(?<string>\"[^\"\\n]*\"?)"
            + "|(?<numero>(?<![\\p{L}\\p{N}_])-?\\d+(?:\\.\\d+)?)"
            + "|(?<palabra>[\\p{L}_][\\p{L}\\p{N}_]*[=!]?)");

    private final JTextPane editor;
    private final Timer espera;

    Resaltador(JTextPane editor) {
        this.editor = editor;
        this.espera = new Timer(150, e -> colorear());
        this.espera.setRepeats(false);
        editor.getDocument().addDocumentListener(this);
    }

    void colorear() {
        StyledDocument doc = editor.getStyledDocument();
        String texto;
        try {
            texto = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            return;
        }
        doc.setCharacterAttributes(0, texto.length(), estilo(TEXTO, false, false), true);
        Matcher m = TOKENS.matcher(texto);
        while (m.find()) {
            int inicio = m.start(), largo = m.end() - m.start();
            if (m.group("comentario") != null) {
                doc.setCharacterAttributes(inicio, largo, estilo(COMENTARIO, false, true), true);
            } else if (m.group("string") != null) {
                doc.setCharacterAttributes(inicio, largo, estilo(STRING, false, false), true);
            } else if (m.group("numero") != null) {
                doc.setCharacterAttributes(inicio, largo, estilo(NUMERO, false, false), true);
            } else {
                Color color = PALABRAS.get(m.group("palabra"));
                if (color != null) {
                    doc.setCharacterAttributes(inicio, largo, estilo(color, true, false), true);
                }
            }
        }
    }

    private static SimpleAttributeSet estilo(Color color, boolean negrita, boolean cursiva) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, negrita);
        StyleConstants.setItalic(a, cursiva);
        return a;
    }

    private void programar() {
        SwingUtilities.invokeLater(espera::restart);
    }

    @Override public void insertUpdate(DocumentEvent e) { programar(); }
    @Override public void removeUpdate(DocumentEvent e) { programar(); }
    @Override public void changedUpdate(DocumentEvent e) { /* cambios de color: se ignoran */ }
}
