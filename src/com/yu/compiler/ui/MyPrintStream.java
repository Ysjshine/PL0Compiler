package com.yu.compiler.ui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.io.OutputStream;
import java.io.PrintStream;

public class MyPrintStream extends PrintStream {

    private JTextComponent component;
    private StringBuilder sb = new StringBuilder();
    public MyPrintStream(OutputStream out, JTextComponent component) {
        super(out);
        this.component = component;
    }

    public void write(byte[] buf, int off, int len) {
        final String message = new String(buf, off, len);
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                sb.append(message);
                component.setText(sb.toString());
            }
        });
    }
}
