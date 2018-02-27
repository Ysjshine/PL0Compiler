package com.yu.compiler;

import java.util.ArrayList;
import java.util.Arrays;

public class PCode {
    private int f;
    private int l;
    private int a;


    /**
     * LIT :将常数值取到运行栈顶，a为常数值
     * OPR: 关系运算和算术运算指令，将栈顶和次栈顶的内容进行运算，
     *      结果存放在次栈顶，此外还可以是读写等特殊功能的指令，具体操作由a给出
     * LOD:将变量放到栈顶。a域为变量在说明层中的相对位置，l为调用层与说明层的层差值
     * STO:将栈顶的内容送入到某变量单元中。
     * CAL:调用过程的指令。a为被调用过程的目标程序入口地址，l为层差
     * INT:为被调用的过程在运行栈中开辟数据区。a为开辟的单元个数。
     * JMP：无条件转移指令，a为转向地址。
     * JPC:条件转移指令，当栈顶的bool值为假时，转向a域的地址，否则顺序执行。
     */
    public static final int LIT = 0;
    public static final int OPR = 1;
    public static final int LOD = 2;
    public static final int STO = 3;
    public static final int CAL = 4;
    public static final int INT = 5;
    public static final int JMP = 6;
    public static final int JPC = 7;

    public static final ArrayList<String> COMMAND = new ArrayList<>(
            Arrays.asList("LIT","OPR","LOD","STO","CAL","INT","JMP","JPC"));

    public PCode(int operator,int level,int addr){
        f = operator;
        l = level;
        a = addr;
    }

    @Override
    public String toString() {
        return COMMAND.get(f)+"\t"+l+","+a;
    }

    public int getF() {
        return f;
    }

    public void setF(int f) {
        this.f = f;
    }

    public int getL() {
        return l;
    }

    public void setL(int l) {
        this.l = l;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}
