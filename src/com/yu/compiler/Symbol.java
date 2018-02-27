package com.yu.compiler;

import java.util.ArrayList;
import java.util.Arrays;

//用于管理所有有关的符号，比如运算符，保留字等等
public class Symbol {
    //算数运算符
    public static final int PLUS_SY = 1;
    public static final int MINUS_SY = 2;
    public static final int MUL_SY = 3;
    public static final int DIV_SY = 4;

    //比较运算符
    public static final int ODD_SY = 5;
    public static final int EQUAL_SY = 6;
    public static final int NON_EQUAL_SY = 7;
    public static final int GREATER_SY = 8;
    public static final int GREATER_EQUAL_SY = 9;
    public static final int LESS_SY = 10;
    public static final int LESS_EQUAL_SY = 11;

    //分界符
    public static final int LEFT_PAR_SY = 12;
    public static final int RIGHT_PAR_SY = 13;
    public static final int COMMA_SY = 14;
    public static final int DOT_SY = 15;
    public static final int SEMI_SY = 16;

    //赋值符号
    public static final int ASSIGN_SY = 17;

    //标识符和数字
    public static final int ID_SY = 18;
    public static final int NUM_SY = 19;

    //关键字或者保留字
    public static final int BEGIN_SY = 20;
    public static final int END_SY = 21;
    public static final int CONST_SY = 22;
    public static final int VAR_SY = 23;
    public static final int PROCEDURE_SY = 24;
    public static final int IF_SY = 25;
    public static final int ELSE_SY = 26;
    public static final int THEN_SY = 27;
    public static final int WHILE_SY = 28;
    public static final int DO_SY = 29;
    public static final int CALL_SY = 30;
    public static final int WRITE_SY = 31;
    public static final int REPEAT_SY = 32;
    public static final int UNTIL_SY = 33;
    public static final int READ_SY = 34;

    //err
    public static final int ERR_SY = -1;

    //关键字
    public static final ArrayList<String> KEY_WORDS
            = new ArrayList<>(
                    Arrays.asList("begin","end","const","var","procedure"
                            ,"if","else","then","while","do"
                            ,"call","write","read","repeat","until"
                            ,"odd"));
    //关键字对应的符号
    public static final ArrayList<Integer> KEY_WORD_INDEX
            = new ArrayList<>(
                    Arrays.asList(BEGIN_SY,END_SY,CONST_SY,VAR_SY,PROCEDURE_SY
                            ,IF_SY,ELSE_SY,THEN_SY,WHILE_SY,DO_SY
                            ,CALL_SY,WRITE_SY,READ_SY,REPEAT_SY,UNTIL_SY
                            ,ODD_SY));

    public static final ArrayList<Character> SINGLE_OP
            = new ArrayList<>(Arrays.asList('+','-','*','/','(',')','.',',',';','='));

    public static final ArrayList<Integer> SINGLE_OP_INDEX
            = new ArrayList<>(Arrays.asList(PLUS_SY,MINUS_SY,MUL_SY,DIV_SY,LEFT_PAR_SY,RIGHT_PAR_SY
            ,DOT_SY,COMMA_SY,SEMI_SY,EQUAL_SY));

    private int symbol;
    private int value;
    private String name;

    public Symbol(int symbol,String name){
        this.symbol = symbol;
        this.name = name;
        if(symbol == NUM_SY){
            value = Integer.parseInt(name);
        }else {
            value = 0;
        }
    }

    public int getSymbol() {
        return symbol;
    }

    public void setSymbol(int symbol) {
        this.symbol = symbol;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " "+symbol+"\n";
    }
}
