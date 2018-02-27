package com.yu.compiler;

import java.util.HashMap;
import java.util.Map;

public class Error {
    public static Map<Integer,String> ERROR_LIST = new HashMap<>();

    static {
        ERROR_LIST.put(-1,"词法有错");
        ERROR_LIST.put(1,"应是=而不是:=");
        ERROR_LIST.put(2,"=后应为数");
        ERROR_LIST.put(3,"标识符后应为=");
        ERROR_LIST.put(4,"const,var,procedure后面应为标识符");
        ERROR_LIST.put(5,"漏掉逗号或分号");
        ERROR_LIST.put(6,"过程说明后的符号不正确");
        ERROR_LIST.put(7,"应为语句");
        ERROR_LIST.put(8,"程序体内语句部分后的符号不正确");
        ERROR_LIST.put(9,"应为句号");
        ERROR_LIST.put(10,"语句之间漏分号");
        ERROR_LIST.put(11,"标识符未说明");
        ERROR_LIST.put(12,"不可向常量或过程赋值");
        ERROR_LIST.put(13,"应为赋值运算符:=");
        ERROR_LIST.put(14,"call 后面应为标识符");
        ERROR_LIST.put(15,"不可调用常量或者变量");
        ERROR_LIST.put(16,"应为then");
        ERROR_LIST.put(17,"应为分号或end");
        ERROR_LIST.put(18,"应为do");
        ERROR_LIST.put(19,"语句后的符号不正确");
        ERROR_LIST.put(20,"应为关系运算符");
        ERROR_LIST.put(21,"表达式内不可有过程标识符");
        ERROR_LIST.put(22,"漏右括号");
        ERROR_LIST.put(23,"因子后不可为此符号");
        ERROR_LIST.put(24,"表达式不能以此符号开始");
        ERROR_LIST.put(30,"这个数太大");
        ERROR_LIST.put(31,"read语句括号括号中标识符不是变量");
        ERROR_LIST.put(32,"嵌套数过多");
        ERROR_LIST.put(33,"repeat后面漏掉了until");
        ERROR_LIST.put(40,"应为左括号");
    }

    private int error;
    private int line;

    /**
     * Error构造函数
     * @param error 错误信息的编码
     * @param line 第几行
     */
    public Error(int error,int line){
        this.error = error;
        this.line = line;
    }

    @Override
    public String toString() {
        return "error: in line "+line+" "+ERROR_LIST.get(error)+"\n";
    }
}
