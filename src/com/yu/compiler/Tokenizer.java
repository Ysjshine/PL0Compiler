package com.yu.compiler;

import java.io.*;

/**
 * 词法分析器
 */
public class Tokenizer {
    private String sourceFile = "";
    private String currentToken = "";
    private char currentChar;
    private int currentIndex;
    private int lineIndex;

    //获取行号
    public int getLineIndex() {
        return lineIndex;
    }

    /**
     * @param filepath 源文件路径
     * @throws IOException
     * @throws FileNotFoundException
     */
    public Tokenizer(String filepath) throws IOException,FileNotFoundException{
        readFile(filepath);
        currentIndex  = 0;
        lineIndex = 1;
        currentChar = sourceFile.charAt(currentIndex);
    }

    //读取文件
    private void readFile(String filepath) throws FileNotFoundException,IOException {
        File file = new File(filepath);
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(file));
        String temp;
        while ((temp = reader.readLine()) != null) {
            sourceFile = sourceFile.concat(temp + "\n");
        }
    }

    //将读出的字符连接到token上
    private void catToken(){
        currentToken = currentToken.concat(Character.toString(currentChar));
    }
    //清空token
    private void clearToken(){
        currentToken = "";
    }
    //读取下一个字符
    private boolean getNext(){
        currentIndex++;
        if(currentIndex>=sourceFile.length()-1){
            return false;
        }
        currentChar = sourceFile.charAt(currentIndex);
        return true;
    }

    private boolean isLetter(){
        return ((currentChar >='a' &&currentChar<='z') ||(currentChar>='A'&&currentChar <='Z'));
    }

    private boolean isDigit(){
        return (currentChar>='0' && currentChar<='9');
    }

    //从源文件中获取一个符号
    public Symbol getSymbol(){
        Symbol symbol = null;
        if(currentIndex>=sourceFile.length()-1){
            return null;
        }
        clearToken();
        while(currentChar == ' '||currentChar == '\t'||currentChar=='\n' || currentChar == '\r'){
            if(currentChar == '\n') lineIndex++;
           boolean ans =  getNext();
           if(!ans) return null;
        }

        if(isLetter()){//开头如果是字母的话，那么后面就应该跟数字或者字母
            catToken();getNext();
            while(isDigit()||isLetter()){
                catToken();getNext();
            }
            if(Symbol.KEY_WORDS.contains(currentToken)){//看看是不是关键字
                int index = Symbol.KEY_WORDS.indexOf(currentToken);
                symbol = new Symbol(Symbol.KEY_WORD_INDEX.get(index),currentToken);
            }else {//不是关键字，那么就是标识符了撒
                symbol = new Symbol(Symbol.ID_SY,currentToken);
            }
        }else if(isDigit()){//如果开头是数字的话，那肯定是数字了
            catToken();getNext();
            while (isDigit()){
                catToken();getNext();
            }
            if(isLetter()){ //如果数字和字母连起来，就像123ABC这种，报错
                while(isLetter()||isDigit()){
                    catToken();getNext();
                }
                symbol = new Symbol(Symbol.ERR_SY,currentToken);
                symbol.setValue(lineIndex);
            }else {//正常数字
                symbol = new Symbol(Symbol.NUM_SY, currentToken);
            }
        }else{//如果是运算符或者分界符开头的话或者是不认识的字符开头
            symbol = getOperator();
        }

        return symbol;
    }

    //如果是运算符或者不认识的字符
    private Symbol getOperator(){
        Symbol symbol = null;
        if(Symbol.SINGLE_OP.contains(currentChar)){//如果是单字符运算符或分界符
            catToken();getNext();
            int index = Symbol.SINGLE_OP.indexOf(currentToken.charAt(0));
            symbol = new Symbol(Symbol.SINGLE_OP_INDEX.get(index),currentToken);
        }else if(currentChar == '>'){//如果有可能是双字符
            catToken();getNext();
            if(sourceFile.charAt(currentIndex) == '='){ //>=
                catToken();getNext();
                symbol = new Symbol(Symbol.GREATER_EQUAL_SY,currentToken);
            }else {//>
                symbol = new Symbol(Symbol.GREATER_SY, currentToken);
            }
        }else if(currentChar == '<'){
            catToken();getNext();
            if(sourceFile.charAt(currentIndex) == '='){//<=
                catToken();getNext();
                symbol = new Symbol(Symbol.LESS_EQUAL_SY,currentToken);
            }else if(sourceFile.charAt(currentIndex) == '>'){//<>
                catToken();getNext();
                symbol = new Symbol(Symbol.NON_EQUAL_SY,currentToken);
            }else {//<
                symbol = new Symbol(Symbol.LESS_SY,currentToken);
            }
        }else if(currentChar == ':'){
            catToken();getNext();
            if(sourceFile.charAt(currentIndex) == '='){//:=
                catToken();getNext();
                symbol = new Symbol(Symbol.ASSIGN_SY,currentToken);
            }else {//出错了，符号不存在
                symbol = new Symbol(Symbol.ERR_SY,currentToken);
                symbol.setValue(lineIndex);
            }
        }else {//这个字符撒子都不是，看都没看见过，出错了撒
            catToken();getNext();
            while(isDigit()||isLetter()){ //比如说__12334,_ABC这些都是错的，读完到运算符或者空格这些
                catToken();getNext();
            }
            symbol = new Symbol(Symbol.ERR_SY,currentToken);
            symbol.setValue(lineIndex);
        }
        return symbol;
    }
}
