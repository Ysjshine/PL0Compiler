package com.yu.compiler.ui;

import com.yu.compiler.*;
import com.yu.compiler.Error;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class IDE extends JFrame {
    private JButton showPcode;
    private JButton runCode;
    private JButton fileButton;
    private JTextArea pcodeTextArea;
    private JTextArea runTextArea;
    private JTextArea errorTextArea;
    private JButton errlabel;
    private JButton clearButton;

    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;

    private ArrayList<Error> errors = null;
    private Tokenizer tokenizer = null;
    private Interpreter interpreter = null;
    private SymbolTable table = null;
    private Parser parser = null;

    private MyPrintStream myPrintStream = null;
    private ByteArrayOutputStream outputStream = null;
    public BlockingQueue<Integer> input = new LinkedBlockingDeque<>();

    public void init(){
        showPcode = new JButton("Show P-code");
        runCode = new JButton("Run");
        pcodeTextArea = new JTextArea();
        runTextArea = new JTextArea();
        errorTextArea = new JTextArea();
        fileButton = new JButton("源代码文件");
        errlabel = new JButton("错误信息");
        clearButton = new JButton("清除");

    }

    public IDE(){
        init();
        setSize(WIDTH,HEIGHT);
        outputStream = new ByteArrayOutputStream();
        myPrintStream = new MyPrintStream(outputStream,runTextArea);
        //System.setOut(myPrintStream);
        add(setpanel());
        setEvent();
    }

    public void setEvent(){
         fileButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 JFileChooser fileChooser = new JFileChooser();
                 fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY );
                 fileChooser.showDialog(new JLabel(), "选择");
                 File file=fileChooser.getSelectedFile();
                 if(file == null) return;
                 try{
                     tokenizer = new Tokenizer(file.getAbsolutePath());
                     interpreter = new Interpreter();
                     table = new SymbolTable();
                     errors = new ArrayList<>();
                     parser = new Parser(tokenizer,interpreter,table,errors);
                     parser.mainProgram();
                     if(errors.size() > 0){
                         StringBuilder sb = new StringBuilder();
                         for (Error err:errors){
                             sb.append(err.toString());
                         }
                         errorTextArea.setText(sb.toString());
                     }else {
                         errorTextArea.setText(file.getAbsolutePath()+"\n编译成功，没有错误");
                     }
                 }catch (Exception eq){
                     eq.printStackTrace();
                 }
             }
         });


         showPcode.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 pcodeTextArea.setText(parser.stringBuilder.toString());
             }
         });

         runCode.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 //runTextArea.setText("");
                 new Thread(new Runnable() {
                     @Override
                     public void run() {
                         try {
                             interpreter.interpret(input, myPrintStream);
                         }catch (IOException ioe){
                             ioe.printStackTrace();
                         }
                     }
                 }).start();
             }

         });

         runTextArea.addKeyListener(new KeyListener() {
             @Override
             public void keyTyped(KeyEvent e) {

             }

             @Override
             public void keyPressed(KeyEvent e) {

             }

             @Override
             public void keyReleased(KeyEvent e) {
                 try {
                     if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                         String s = runTextArea.getText();
                         String[] res =  s.split("\\n");
                         System.out.println(res[res.length-1]);
                         byte[] buf = (res[res.length-1]+"\n").getBytes();
                         myPrintStream.write(buf);myPrintStream.flush();
                         int data = Integer.parseInt(res[res.length-1]);
                         input.put(data);
                     }
                 }catch (NumberFormatException e1){
                     System.out.println("Runtime Error:输入数字格式不正确");
                     try {
                         byte[] buf = ("Runtime Error:输入数字格式不正确").getBytes();
                         myPrintStream.write(buf);
                         myPrintStream.flush();
                     }catch (Exception e4){

                     }
                 }catch (InterruptedException e2){

                 }catch (IOException e3){

                 }
             }
         });

         clearButton.addActionListener(new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 errorTextArea.setText("");
                 runTextArea.setText("");
                 pcodeTextArea.setText("");
                 try {
                     myPrintStream.close();
                     outputStream = new ByteArrayOutputStream();
                     myPrintStream = new MyPrintStream(outputStream,runTextArea);
                 }catch (Exception e1){
                     e1.printStackTrace();
                 }
             }
         });

    }

    public Panel setpanel(){
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());
        panel.add(fileButton,BorderLayout.NORTH);

        Panel infoPanel = new Panel(new GridLayout(1,3));
        Panel runPanel = new Panel(new BorderLayout());
        Panel showCodePanel = new Panel(new BorderLayout());
        Panel errorPanel = new Panel(new BorderLayout());

        runPanel.add(runCode,BorderLayout.NORTH);
        runPanel.add(new JScrollPane(runTextArea),BorderLayout.CENTER);

        showCodePanel.add(showPcode,BorderLayout.NORTH);
        showCodePanel.add(new JScrollPane(pcodeTextArea),BorderLayout.CENTER);

        errorPanel.add(errlabel,BorderLayout.NORTH);
        errorPanel.add(new JScrollPane(errorTextArea),BorderLayout.CENTER);

        infoPanel.add(errorPanel);
        infoPanel.add(showCodePanel);
        infoPanel.add(runPanel);

        panel.add(infoPanel,BorderLayout.CENTER);
        panel.add(clearButton,BorderLayout.SOUTH);
        return panel;
    }
}
