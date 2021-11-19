package com.illcode.jedit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SwitchBuffer implements KeyListener
{
    JDialog dialog;
    JFrame owner;
    JLabel label;
    JTextArea textArea;

    void initComponents(JFrame owner) {
        this.owner = owner;

        dialog = new JDialog(owner, true);
        label = new JLabel();
        textArea = new JTextArea();

        //======== dialog ========
        dialog.setTitle("Switch Buffer");
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new FormLayout(
            "$rgap, 131dlu, $rgap",
            "$rgap, default, $lgap, fill:default:grow, $rgap"));

        //---- label ----
        label.setText("Choose Buffer:");
        contentPane.add(label, CC.xy(2, 2));

        //---- textArea ----
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setOpaque(false);
        contentPane.add(textArea, CC.xy(2, 4));
    }

    void show() {
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        textArea.addKeyListener(this);
        textArea.requestFocusInWindow();
        dialog.setVisible(true);  // blocks until hidden
        textArea.removeKeyListener(this);
    }

    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();

        dialog.setVisible(false);
    }

    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
}
