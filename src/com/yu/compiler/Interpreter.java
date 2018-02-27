package com.yu.compiler;

import com.yu.compiler.ui.MyPrintStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class Interpreter {
    //pcode数组大小
    private static final int MAX_PCODE = 1000;
    //运行栈的大小
    private static final int STACK_SIZE = 1000;
    //存放虚拟机代码
    private PCode[] pcodes ;
    //虚拟机代码指针
    private int pcodeIndex = 0;

    public int getPcodeIndex() {
        return pcodeIndex;
    }

    public PCode[] getPcodes() {
        return pcodes;
    }

    //构造函数
    public Interpreter(){
        pcodes = new PCode[MAX_PCODE];
    }

    //生成pcode，并且存入pcodes中
    public void gen(int f,int l,int a){
        if(pcodeIndex >= MAX_PCODE){
           // System.out.println("Error:Program is too long");
        }
        PCode pCode = new PCode(f,l,a);
        pcodes[pcodeIndex] = pCode;
        //System.out.println(pcodeIndex+": "+pCode);
        pcodeIndex++;
    }
    //展示虚拟机代码
    public String showPcodes(int start){
        StringBuilder sb = new StringBuilder();
        for(int i = start;i<pcodeIndex;i++){
            System.out.println(pcodes[i]+"\n");
            sb.append("   "+pcodes[i].toString()+'\n');
        }
        return sb.toString();
    }

    /**
     * 解释并执行pcode指令
     * @param input 输入整数的存放处，使用阻塞队列实现，主要是为了方便界面
     * @param out 输出
     * @throws IOException
     */
    public void interpret(BlockingQueue<Integer> input, MyPrintStream out) throws IOException{
        /**
         * p:程序指令寄存器
         * b:基地址寄存器
         * t:topstack registers
         */
        int p = 0,b=1,t = 0;

        PCode pCode = null;
        //运行栈
        int[] stack = new int[STACK_SIZE];
        byte[] buf = "\n----PL0 start running----\n".getBytes();
        out.write(buf);out.flush();
        System.out.println("\n----PL0 start running----\n");
        for(int i=0;i<STACK_SIZE;i++){
            stack[i]= 0;
        }

        do{
            pCode = pcodes[p];
            p++;
            switch (pCode.getF()){
                case PCode.LIT: //将常数值取到栈顶
                    t = t+1;
                    stack[t] = pCode.getA();
                    break;
                case PCode.OPR:
                    switch (pCode.getA()){
                        case 0: //返回
                            t = b-1;
                            p = stack[t+3];
                            b = stack[t+2];
                            break;
                        case 1: //取反
                            stack[t] = -stack[t];
                            break;
                        case 2: //加法
                            t = t-1;
                            stack[t] = stack[t] + stack[t+1];
                            break;
                        case 3://减法
                            t = t-1;
                            stack[t] = stack[t] - stack[t+1];
                            break;
                        case 4://乘法
                            t = t-1;
                            stack[t] = stack[t]*stack[t+1];
                            break;
                        case 5://除法
                            t = t-1;
                            if(stack[t+1] == 0){
                                System.out.println("Runtime Error: Divide by 0.");
                                buf = "Runtime Error: Divide by 0.".getBytes();
                                out.write(buf);out.flush();
                            }
                            stack[t] = stack[t]/stack[t+1];
                            break;
                        case 6://odd运算
                            if(stack[t]%2 == 0) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 8://判断相等
                            t = t-1;
                            if(stack[t] == stack[t+1]) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 9://判断是否不等
                            t = t-1;
                            if(stack[t] == stack[t+1]) stack[t] = 0;
                            else stack[t] = 1;
                            break;
                        case 10://判断是否小于
                            t = t-1;
                            if(stack[t]<stack[t+1]) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 11://判断是否大于等于
                            t = t-1;
                            if(stack[t]>=stack[t+1]) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 12://判断是否大于
                            t = t-1;
                            if(stack[t]>stack[t+1]) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 13://判断是否小于等于
                            t = t-1;
                            if(stack[t]<=stack[t+1]) stack[t] = 1;
                            else stack[t] = 0;
                            break;
                        case 14://输出栈顶元素
                            System.out.printf("%d ",stack[t]);
                            buf = (stack[t] + " ").getBytes();
                            out.write(buf);out.flush();
                            t = t-1;
                            break;
                        case 15://输出换行
                            System.out.println("");
                            buf = ("\n").getBytes();
                            out.write(buf);out.flush();
                            break;
                        case 16://输入一个元素
                            t = t+1;
                            System.out.println("input a number:");
                            buf = ("input a number:\n").getBytes();
                            out.write(buf);out.flush();
                            try {
                                stack[t] = input.take();
                                input.poll();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            break;
                        default:break;
                    }
                    break;
                case PCode.LOD: //将变量放到栈顶
                    t = t+1;
                    stack[t] = stack[base(pCode.getL(),b,stack)+pCode.getA()];
                    break;
                case PCode.STO: //将栈顶内容送入变量
                    stack[base(pCode.getL(),b,stack)+pCode.getA()] = stack[t];
                    t = t-1;
                    break;
                case PCode.CAL://调用过程
                    stack[t+1] = base(pCode.getL(),b,stack); //填写静态链SL
                    stack[t+2] = b;//填写动态链
                    stack[t+3] = p;//填写返回地址
                    b = t+1;//被调用过程的基地址
                    p = pCode.getA();//设置入口地址
                    break;
                case PCode.INT: //开辟空间
                    t = t+pCode.getA();break;
                case PCode.JMP: //无条件跳转
                    p = pCode.getA();break;
                case PCode.JPC://当栈顶的值为假时，转向a域的地址，否则顺序执行
                    if(stack[t] == 0) p = pCode.getA();
                    t = t-1;
                    break;
            }
        }while (p!=0);
        //System.out.println("\n-----run successfully-----");
    }

    /**
     *求上一层的基址
     * @param l 层数
     * @param b 基址
     * @param stack 运行栈
     * @return 找到的基址
     */
    private int base(int l,int b,int[] stack){
        int b1 = b;
        while(l>0){
            b1 = stack[b1];
            l = l-1;
        }
        return b1;
    }
}
