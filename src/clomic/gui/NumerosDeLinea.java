package clomic.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;

/**
 * La columna con los números de línea a la izquierda del editor.
 * La línea con error se marca en rojo, para que coincida con el mensaje "error on line N".
 */
final class NumerosDeLinea extends JComponent implements DocumentListener {

    private static final Color FONDO = new Color(0xF6F8FA);
    private static final Color NUMERO = new Color(0x8C959F);
    private static final Color BORDE = new Color(0xD0D7DE);
    private static final Color ERROR = new Color(0xCF222E);

    private final JTextPane editor;
    private int lineaError = -1;

    NumerosDeLinea(JTextPane editor) {
        this.editor = editor;
        editor.getDocument().addDocumentListener(this);
        setFont(editor.getFont());
    }

    void marcarError(int linea) {
        lineaError = linea;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int lineas = editor.getDocument().getDefaultRootElement().getElementCount();
        int digitos = Math.max(3, String.valueOf(lineas).length());
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.charWidth('0') * digitos + 22, editor.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Rectangle visible = g.getClipBounds();
        g.setColor(FONDO);
        g.fillRect(visible.x, visible.y, visible.width, visible.height);
        g.setColor(BORDE);
        g.drawLine(getWidth() - 1, visible.y, getWidth() - 1, visible.y + visible.height);

        FontMetrics fm = g.getFontMetrics(getFont());
        Element raiz = editor.getDocument().getDefaultRootElement();
        for (int i = 0; i < raiz.getElementCount(); i++) {
            try {
                Rectangle r = editor.modelToView2D(raiz.getElement(i).getStartOffset()).getBounds();
                if (r.y + r.height < visible.y || r.y > visible.y + visible.height) continue;
                String n = String.valueOf(i + 1);
                boolean conError = (i + 1) == lineaError;
                g.setFont(conError ? getFont().deriveFont(java.awt.Font.BOLD) : getFont());
                g.setColor(conError ? ERROR : NUMERO);
                int x = getWidth() - fm.stringWidth(n) - 10;
                g.drawString(n, x, r.y + fm.getAscent());
            } catch (Exception ignorado) {
                // la vista todavía no está lista
            }
        }
    }

    private void actualizar() {
        revalidate();
        repaint();
    }

    @Override public void insertUpdate(DocumentEvent e) { actualizar(); }
    @Override public void removeUpdate(DocumentEvent e) { actualizar(); }
    @Override public void changedUpdate(DocumentEvent e) { }
}
