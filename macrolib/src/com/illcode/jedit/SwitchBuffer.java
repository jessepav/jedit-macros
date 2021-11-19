package com.illcode.jedit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SwitchBuffer implements KeyListener
{
    Buffer buffer;
    View view;
    EditPane editPane;
    JEditTextArea textArea;
    DockableWindowManager wm;
    String scriptPath;

    JDialog dialog;
    JFrame owner;
    JLabel label;
    JTextArea bufferTextArea;

    void initComponents(JFrame owner) {
        this.owner = owner;

        dialog = new JDialog(owner, true);
        label = new JLabel();
        bufferTextArea = new JTextArea();

        //======== dialog ========
        dialog.setTitle("Switch Buffer");
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new FormLayout(
            "$rgap, 131dlu, $rgap",
            "$rgap, default, $lgap, fill:default:grow, $rgap"));

        //---- label ----
        label.setText("Choose Buffer:");
        label.setFocusable(false);
        contentPane.add(label, CC.xy(2, 2));

        //---- textArea ----
        bufferTextArea.setEditable(false);
        bufferTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bufferTextArea.setOpaque(false);
        contentPane.add(bufferTextArea, CC.xy(2, 4));
    }

    void show() {
        StringBuilder sb = new StringBuilder(256);

        bufferTextArea.setText(sb.toString());
        sb = null;

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        bufferTextArea.addKeyListener(this);
        bufferTextArea.requestFocusInWindow();
        dialog.setVisible(true);  // blocks until hidden
        bufferTextArea.removeKeyListener(this);
    }

    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        c = Character.toUpperCase(c);

        dialog.setVisible(false);
    }

    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
}
