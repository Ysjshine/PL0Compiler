package com.yu.compiler;
import java.util.ArrayList;
import java.util.HashSet;

public class Parser {

    //分词器，用于得到symbol以及行数
    private Tokenizer tokenizer;
    //当前读到的符号
    private Symbol symbol;
    //翻译器，用于保存生成的Pcode指令
    private Interpreter interpreter;
    //符号表
    private SymbolTable table;
    //保存错误信息
    private ArrayList<Error> errors;

    //记录Pcode，用于GUI的输出
    public StringBuilder stringBuilder = new StringBuilder();

    /**
     * 计算每个变量在运行栈中相对ben过程的基地址的偏移量
     * 放在符号表中的adr字段
     * 生成目标代码时放在pcode的a字段
     * 初始化为0；
     */
    private int dx = 0;
    /**
     * 语法分析器的构造函数
     * @param tokenizer 词法分析器
     * @param interpreter 目标代码生成器
     * @param table 符号表
     * @param errors 错误列表
     */
    public Parser(Tokenizer tokenizer, Interpreter interpreter, SymbolTable table, ArrayList<Error> errors) {
        this.tokenizer = tokenizer;
        this.interpreter = interpreter;
        this.table = table;
        this.errors = errors;
        getNextSymbol();
    }

    /**
     * 用于从词法分析器中读取符号
     * 如果词法有错误，那么保存到错误列表并重新读取下一个符号
     */
    public void getNextSymbol(){
        symbol = tokenizer.getSymbol();
        while (symbol != null && symbol.getSymbol()==Symbol.ERR_SY){
            errors.add(new Error(-1,symbol.getValue()));
            symbol = tokenizer.getSymbol();
        }
    }

    /**
     *  比如说如果是const ;这种，就不get下一个了。如果是 const begin这种，就需要跳过begin
     */
    public void isSemi(){
        if(symbol.getSymbol()!=Symbol.SEMI_SY){
            getNextSymbol();
        }
    }

    /**
     *当进入某个语法单位时，调用test方法，检查当前符号是否属于该语法单位的开始符号集合
     * 若不属于，则滤去开始符号和后跟符号集合外的所有符号。
     * 在语法分析结束时，调用test方法检查当前符号是否属于调用该语法单位应该有的
     * 后跟符号集合，若不属于，则滤去后跟符号和开始符号外的所有符号
     * @param first first符号或follow
     * @param second first符号或follow
     * @param error 错误代码
     */
    public void test(HashSet<Integer> first, HashSet<Integer> second , int error){
        if(symbol!= null&&!first.contains(symbol.getSymbol())){
            errors.add(new Error(error,tokenizer.getLineIndex()));
            first.addAll(second);
            while(symbol!=null && !first.contains(symbol.getSymbol())){
                getNextSymbol();
            }
        }
    }

    /**
     * 分析产生式<程序> ::= <分程序>.
     * 在这个之前需要调用一次getNextSymbol();
     */
    public void mainProgram(){
        HashSet<Integer> next = new HashSet<>();
        next.addAll(FirstSet.DECLARE_FIRST);
        next.addAll(FirstSet.STAT_FIRST);
        next.add(Symbol.DOT_SY);

        block(0,next);

        if(symbol==null || symbol.getSymbol() != Symbol.DOT_SY){
            //应为句号
            errors.add(new Error(9,tokenizer.getLineIndex()));
        }
       // System.out.println("-------------compile successfully-------------");
    }

    /**
     * 分析分程序
     * <分程序> ::= [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>;
     * @param level 分程序在第几层
     * @param follow 分程序的FOLLOW集合
     */
    public void block(int level,HashSet<Integer> follow){
        //判断当前分程序所在层次，如果层次数大于3，那么报错
        if(level > SymbolTable.NEST_MAX_SIZE){
            errors.add(new Error(32,tokenizer.getLineIndex()));
        }

        HashSet<Integer> next = null;
        //记录当前层的地址，以及本层符号的起始位置
        int dx0 = dx;
        int tx0 = table.getTableIndex();

        //每一层最开始的位置有三个空间用于存放静态链SL、动态链DL和返回地址RA
        dx = 3;

        //将当前pcode代码的地址传给符号表的adr字段
        table.get(table.getTableIndex()).setAddress(interpreter.getPcodeIndex());
        interpreter.gen(PCode.JMP,0,0);

        //开始分析[说明部分]
        while(symbol!=null&&FirstSet.DECLARE_FIRST.contains(symbol.getSymbol())){
            /**
             * 常量说明部分
             *  <常量说明部分> ::= const<常量定义>{,<常量定义>};
             */
            if(symbol.getSymbol() == Symbol.CONST_SY){
                getNextSymbol();
                constDeclaration(level);//<常量定义>
                //如果下面是逗号的话,继续添加常量
                while(symbol!=null&&symbol.getSymbol() == Symbol.COMMA_SY){
                    getNextSymbol();
                    constDeclaration(level);
                }
                //常量说明部分结束应该是分号，如果不是则报错
                if(symbol!=null&&symbol.getSymbol() == Symbol.SEMI_SY){
                    getNextSymbol();
                }else {
                    //漏了逗号或者分号
                    errors.add(new Error(5,tokenizer.getLineIndex()));
                }
            }
            /**
             * 变量说明部分
             *  <变量说明部分> ::= var<标识符>{,<标识符>};
             */
            if(symbol!=null&&symbol.getSymbol() == Symbol.VAR_SY){
                getNextSymbol();
                variableDeclaration(level);//变量定义
                //如果下面是逗号的话，继续添加变量。
                while(symbol!=null&&symbol.getSymbol() == Symbol.COMMA_SY){
                    getNextSymbol();
                    variableDeclaration(level);
                }
                //变量说明部分结束应该是分号
                if(symbol!=null&&symbol.getSymbol() == Symbol.SEMI_SY){
                    getNextSymbol();
                }else {
                    //漏了逗号或者分号
                    errors.add(new Error(5,tokenizer.getLineIndex()));
                }
            }
            /**
             * 过程说明部分
             * <过程说明部分> ::= <过程首部><分程序>;{<过程说明部分>}
             *<过程首部> ::= procedure<标识符>;
             */
            while(symbol!= null&&symbol.getSymbol() == Symbol.PROCEDURE_SY){
                getNextSymbol();
                //解析<过程首部>
                procedureHeadIdentDeclaration(level);
                //过程首部应该以分号结尾
                if(symbol!=null&&symbol.getSymbol() == Symbol.SEMI_SY){
                    getNextSymbol();
                }else {
                    //漏了逗号或者分号
                    errors.add(new Error(5,tokenizer.getLineIndex()));
                }

                //解析<分程序>，分程序层次数要加1
                next = new HashSet<>(follow);
                //补充Block的FOLLOW集，将分号加进去
                next.add(Symbol.SEMI_SY);
                block(level+1,next);

                //判断是不是以分号结束的
                if(symbol!=null&&symbol.getSymbol() == Symbol.SEMI_SY){
                    getNextSymbol();
                    //判断接下来的符号是不是语句或者嵌套的分程序
                    next = new HashSet<>(FirstSet.STAT_FIRST);
                    next.add(Symbol.ID_SY);
                    next.add(Symbol.PROCEDURE_SY);
                    test(next,follow,6);
                }else {
                    //漏了逗号或者分号
                    errors.add(new Error(5,tokenizer.getLineIndex()));
                }

            }
            /**
             * 说明部分分析完了后，然后就看看follow的是不是正确的语法成分
             * 判断后面是不是语句
             */
            next = new HashSet<>(FirstSet.STAT_FIRST);
            next.add(Symbol.ID_SY);
            HashSet<Integer> declare = new HashSet<>(FirstSet.DECLARE_FIRST);
            //应为语句
            test(next,declare,7);
        }

        SymbolTableItem item1 = table.get(tx0);
        int addr  = item1.getAddress();
        interpreter.getPcodes()[addr].setA(interpreter.getPcodeIndex());
        item1.setAddress(interpreter.getPcodeIndex());
        item1.setSize(dx);
        //记录当前虚拟机代码的位置
        int cx0 = interpreter.getPcodeIndex();

        //分配内存的Pcode指令，INT 0，dx
        interpreter.gen(PCode.INT,0,dx);

        //分析<语句>，将分号和end符号添加到FOLLOW集中
        next = new HashSet<>(follow);
        next.add(Symbol.SEMI_SY);
        next.add(Symbol.END_SY);
        statement(level,next);

        //语句分析完成后，生成操作数为0的OPR指令
        interpreter.gen(PCode.OPR,0,0);

        //程序体内语句部分后的符号不正确
        test(follow,new HashSet<>(),8);
        //显示代码
        String s = interpreter.showPcodes(cx0);
        stringBuilder.append(s);
        //恢复之前保存好的状态，如当前层的地址以及符号表的指针。
        dx = dx0;
        table.setTableIndex(tx0);

    }

    /**
     * <常量定义> ::= <标识符>=<无符号整数>
     * @param level 常量在第几层
     */
    public void constDeclaration(int level){
        if(symbol!= null&&symbol.getSymbol() == Symbol.ID_SY){
            Symbol bak = symbol;
            getNextSymbol();
            if(symbol!=null&&(symbol.getSymbol() == Symbol.EQUAL_SY || symbol.getSymbol() == Symbol.ASSIGN_SY)){
                if(symbol!=null&&symbol.getSymbol() == Symbol.ASSIGN_SY){
                    //应是=而不是:=
                    errors.add(new Error(1,tokenizer.getLineIndex()));
                }
                getNextSymbol();
                if(symbol!=null&&symbol.getSymbol() == Symbol.NUM_SY){
                    bak.setValue(symbol.getValue());
                    table.enter(bak,0,level,dx);
                    getNextSymbol();
                }else {
                    //=后应为数
                    errors.add(new Error(2,tokenizer.getLineIndex()));
                    isSemi();
                }
            }else {
                //标识符后应为=
                errors.add(new Error(3,tokenizer.getLineIndex()));
                isSemi();
            }
        }else {
            //const,var,procedure后面应为标识符
            errors.add(new Error(4,tokenizer.getLineIndex()));
            isSemi();
        }
    }

    /**
     * 变量说明里面的<标识符>
     * <变量说明部分>::= var<标识符>{,<标识符>};
     * @param level 变量在第几层
     */
    public void variableDeclaration(int level){
        if(symbol!= null&&symbol.getSymbol() == Symbol.ID_SY){
            //登录符号表，类型为1，变量
            table.enter(symbol,1,level,dx);
            dx++;
            getNextSymbol();
        }else {
            //const,var,procedure后面应为标识符
            errors.add(new Error(4,tokenizer.getLineIndex()));
            isSemi();
        }
    }

    /**
     * 过程首部里面的<标识符>
     * <过程首部> ::= procedure<标识符>;
     * @param level 过程所在层数
     */
    public void procedureHeadIdentDeclaration(int level){
        if(symbol!= null&&symbol.getSymbol() == Symbol.ID_SY){
            //登录符号表，类型为2 ，过程
            table.enter(symbol,2,level,dx);
            getNextSymbol();
        }else {
            //const,var,procedure后面应为标识符
            errors.add(new Error(4,tokenizer.getLineIndex()));
            isSemi();
        }
    }

    /**
     * 分析<语句>;
     * <语句> ::= <赋值语句>|<条件语句>|<当型循环语句>|<过程调用语句>|<读语句>|<写语句>|<复合语句>|<重复语句>|<空>
     * @param level 语句所在层次
     * @param follow 语句的FOLLOW符号集
     */
    public void statement(int level,HashSet<Integer> follow){
        if(symbol!= null) {
            if (symbol.getSymbol() == Symbol.ID_SY) {
                //赋值语句
                assignStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.READ_SY) {
                //读语句
                readStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.WRITE_SY) {
                //写语句
                writeStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.CALL_SY) {
                //过程调用语句
                callStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.IF_SY) {
                //条件语句
                ifStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.WHILE_SY) {
                //当型循环语句
                whileStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.BEGIN_SY) {
                //复合语句
                beginStatement(level, follow);
            } else if (symbol.getSymbol() == Symbol.REPEAT_SY) {
                //重复语句
                repeatStatement(level, follow);
            } else {
                //语句后的符号不正确
                test(follow, new HashSet<>(), 19);
            }
        }

    }

    /**
     * 分析<赋值语句>;
     * <赋值语句> ::= <标识符>:=<表达式>
     * @param level 语句所在层次;
     * @param follow FOLLOW符号集
     */
    public void assignStatement(int level,HashSet<Integer> follow){
        //首先，应从符号表中找出变量的位置
        int pos = table.position(symbol.getName());
        SymbolTableItem item = null;
        //如果符号表中找到了这个标识符的定义
        if(pos>0){
            item = table.get(pos);
            //如果这个符号是变量
            if(item.getType() == 1){
                getNextSymbol();
                if(symbol!=null&&symbol.getSymbol() == Symbol.ASSIGN_SY){
                    getNextSymbol();
                }else {
                    //应为赋值运算符
                    errors.add(new Error(13,tokenizer.getLineIndex()));
                    getNextSymbol();
                }
                HashSet<Integer> next = new HashSet<>(follow);
                //表达式解析
                expression(level,next);
                interpreter.gen(PCode.STO,level-item.getLevel(),item.getAddress());
            }else {
                //不可向常量或者过程赋值
                errors.add(new Error(12,tokenizer.getLineIndex()));
                //就算不是变量，后面的还可以继续解析，只是不生成p_code
                getNextSymbol();
                if(symbol!=null&&symbol.getSymbol() == Symbol.ASSIGN_SY){
                    getNextSymbol();
                }else {
                    //应为赋值运算符
                    errors.add(new Error(13,tokenizer.getLineIndex()));
                    getNextSymbol();
                }
                HashSet<Integer> next = new HashSet<>(follow);
                //表达式解析
                expression(level,next);

            }
        }else {
            //未在符号表中找到，则标识符未定义
            errors.add(new Error(11,tokenizer.getLineIndex()));
            //就算没有定义，后面的还可以继续解析，只是不生成p_code
            getNextSymbol();
            if(symbol!=null&&symbol.getSymbol() == Symbol.ASSIGN_SY){
                getNextSymbol();
            }else {
                //应为赋值运算符
                errors.add(new Error(13,tokenizer.getLineIndex()));
                getNextSymbol();
            }
            HashSet<Integer> next = new HashSet<>(follow);
            //表达式解析
            expression(level,next);
        }
    }

    /**
     * 解析表达式
     * <表达式> ::= [+|-]<项>{<加法运算符><项>}
     * @param level 所在层次
     * @param follow FOLLOW集合
     */
    public void expression(int level,HashSet<Integer> follow){
        //开始是+或者-
        if(symbol!=null&& (symbol.getSymbol() == Symbol.PLUS_SY || symbol.getSymbol() == Symbol.MINUS_SY)){
            int type = symbol.getSymbol();
            getNextSymbol();
            HashSet<Integer> next = new HashSet<>(follow);
            next.add(Symbol.MINUS_SY);
            next.add(Symbol.PLUS_SY);
            //对<项>的分析
            term(level,next);
            if(symbol!=null&&type == Symbol.MINUS_SY){
                //如果是减号，对数字进行取反
                interpreter.gen(PCode.OPR,0,1);
            }
        }else {
            //否则直接分析<项>
            HashSet<Integer> next = new HashSet<>(follow);
            next.add(Symbol.MINUS_SY);
            next.add(Symbol.PLUS_SY);
            term(level,next);
        }

        // 分析{<加法运算符><项>}
        while(symbol!=null&&(symbol.getSymbol() == Symbol.PLUS_SY || symbol.getSymbol() == Symbol.MINUS_SY)){
            int type = symbol.getSymbol(); //保存当前的加法运算符，以便后面使用
            getNextSymbol();
            HashSet<Integer> next = new HashSet<>(follow);
            next.add(Symbol.MINUS_SY);
            next.add(Symbol.PLUS_SY);
            //对<项>的分析
            term(level,next);
            int oper = 0;
            if(type == Symbol.PLUS_SY) oper = 2;
            else if(type == Symbol.MINUS_SY) oper = 3;
            //opr 0 2:执行加法,opr 0 3:执行减法
            interpreter.gen(PCode.OPR,0,oper);
        }

    }

    /**
     * 对<项>的分析
     * <项> ::= <因子>{<乘法运算符><因子>}
     * @param level <项>所在层次
     * @param follow FOLLOW符号集合
     */
    public void term(int level,HashSet<Integer> follow){
        //首先是对<因子>的分析
        HashSet<Integer> next = new HashSet<>(follow);
        next.add(Symbol.MUL_SY);
        next.add(Symbol.DIV_SY);
        factor(level,follow);

        //分析{<乘法运算符><因子>}
        while (symbol!=null&&(symbol.getSymbol() == Symbol.MUL_SY || symbol.getSymbol() == Symbol.DIV_SY)) {
            int type = symbol.getSymbol();
            getNextSymbol();
            factor(level, follow);
            int oper = 0;
            if(type == Symbol.MUL_SY) oper = 4;
            else if(type == Symbol.DIV_SY) oper = 5;
            //乘法:OPR 0 4 ,除法:OPR 0 5
            interpreter.gen(PCode.OPR, 0, oper);
        }
    }

    /**
     * 分析<因子>
     * <因子> ::= <标识符>|<无符号整数>|'('<表达式>')'
     * @param level 坐在层次
     * @param follow FOLLOW集合
     */
    public void factor(int level,HashSet<Integer> follow){
        HashSet<Integer> factorFirst = new HashSet<>(FirstSet.FACTOR_FIRST);
        //表达式不能以此开始
        test(factorFirst,follow,24);

        //是标识符
        if(symbol!=null&&(symbol.getSymbol() == Symbol.ID_SY)){
            /*查符号表，得到当前符号在符号表中的位置，如果
             *位置大于等于0，那么查找成功
             */
            int pos = table.position(symbol.getName());
            if(pos>0){
                SymbolTableItem item = table.get(pos);
                if(item.getType() == 0){
                    //如果是常量,将常数值取到运行栈顶.
                    interpreter.gen(PCode.LIT,0,item.getValue());
                }
                else if(item.getType()==1){
                    //如果是变量,将栈顶的内容送入到变量单元中。
                    interpreter.gen(PCode.LOD,level-item.getLevel(),item.getAddress());
                }
                else {
                    //如果都不是的话,出错，表达式内不可有过程标识符
                    errors.add(new Error(21,tokenizer.getLineIndex()));
                }
            }else {
                //标识符未说明
                errors.add(new Error(11,tokenizer.getLineIndex()));
            }
            getNextSymbol();
        }
        //是无符号整数
        else if (symbol!=null&&symbol.getSymbol() == Symbol.NUM_SY){
            int value = symbol.getValue();
            //数越界处理
            if(value >= SymbolTable.MAX_NUM){
                //这个数太大
                errors.add(new Error(30,tokenizer.getLineIndex()));
            }
            else {
                //取常数值到栈顶
                interpreter.gen(PCode.LIT, 0, value);
            }
            getNextSymbol();
        }
        //如果是表达式
        else if(symbol!=null&&symbol.getSymbol() == Symbol.LEFT_PAR_SY){
            getNextSymbol();
            HashSet<Integer> next = new HashSet<>(follow);
            //将右括号添加到FOLLOW集
            next.add(Symbol.RIGHT_PAR_SY);
            //分析表达式
            expression(level,next);
            if(symbol!=null&&symbol.getSymbol() == Symbol.RIGHT_PAR_SY){
                getNextSymbol();
            }else {
                //缺少右括号
                errors.add(new Error(22,tokenizer.getLineIndex()));
            }
        }else {
            //检查因子后是否出现了不可以出现的符号
            test(follow,factorFirst,23);
        }
    }


    /**
     * 分析<条件>
     * <条件> ::= <表达式><关系运算符><表达式>|odd<表达式>
     * @param level 所属层次
     * @param follow FOLLOW集
     */
    public void conditionStatement(int level,HashSet<Integer> follow){
//        System.out.println("condition");
        //如果是单目运算符odd
        if(symbol!=null&&symbol.getSymbol() == Symbol.ODD_SY){
            getNextSymbol();
            expression(level,follow);
            //OPR 0,6表示odd
            interpreter.gen(PCode.OPR,0,6);
        }else {
            HashSet<Integer> next = new HashSet<>();
            HashSet<Integer> next1 = new HashSet<>(follow);
            next.add(Symbol.EQUAL_SY);next.add(Symbol.NON_EQUAL_SY);
            next.add(Symbol.LESS_SY);next.add(Symbol.LESS_EQUAL_SY);
            next.add(Symbol.GREATER_SY);next.add(Symbol.GREATER_EQUAL_SY);
            next1.addAll(next);
            //分析<表达式>
            expression(level,next1);
            if(symbol!=null&&next.contains(symbol.getSymbol())){
                int type = symbol.getSymbol();
                getNextSymbol();
                expression(level,next1);
                int oper = 0;
                if(type == Symbol.EQUAL_SY) oper = 8;
                else if (type == Symbol.NON_EQUAL_SY) oper = 9;
                else if(type == Symbol.LESS_SY) oper = 10;
                else if(type == Symbol.GREATER_EQUAL_SY) oper = 11;
                else if(type == Symbol.GREATER_SY) oper = 12;
                else if(type == Symbol.LESS_EQUAL_SY) oper = 13;
                /**
                 * oper:
                 * 8:表示=操作；9：表示不等操作；10：表示小于；11：表示大于等于；12：表示大于
                 * 13：表示小于等于
                 */
                interpreter.gen(PCode.OPR,0,oper);
            }else {
                //应为关系运算符
                errors.add(new Error(20,tokenizer.getLineIndex()));
                //虽然不是关系运算符，但是右边还是有可能是正常的表达式
                getNextSymbol();
                expression(level,next1);
            }
        }
    }

    /**
     * 分析<当型循环语句>
     * <当型循环语句> ::= while<条件>do<语句>
     * @param level 所在层次
     * @param follow FOLLOW集合
     */
    public void whileStatement(int level,HashSet<Integer> follow){
//        System.out.println("while");
        //保存判断条件出现的位置，即当前代码段分配的地址
        int cx = interpreter.getPcodeIndex();
        getNextSymbol();
        HashSet<Integer> next = new HashSet<>(follow);
        //将do添加到follow集中
        next.add(Symbol.DO_SY);
        //解析条件
        conditionStatement(level,next);
        int cx1 = interpreter.getPcodeIndex();
        //无条件跳转
        interpreter.gen(PCode.JPC,0,0);
        if(symbol!= null&&symbol.getSymbol() == Symbol.DO_SY){
            getNextSymbol();
        }else {
            //应为do
            errors.add(new Error(18,tokenizer.getLineIndex()));
            getNextSymbol();
        }
        statement(level,follow);
        //跳转到循环开始的位置
        interpreter.gen(PCode.JMP,0,cx);
        interpreter.getPcodes()[cx1].setA(interpreter.getPcodeIndex());
    }

    /**
     * 分析<重复语句>;
     * <重复语句> ::= repeat<语句>{;<语句>}until<条件>
     * @param level
     * @param follow
     */
    public void repeatStatement(int level,HashSet<Integer> follow){
        int cx = interpreter.getPcodeIndex();
        getNextSymbol();
        HashSet<Integer> next = new HashSet<>(follow);
        next.add(Symbol.SEMI_SY);
        next.add(Symbol.UNTIL_SY);
        statement(level,follow);
        while(symbol!=null&&(FirstSet.STAT_FIRST.contains(symbol.getSymbol())
                ||symbol.getSymbol() == Symbol.SEMI_SY)){
            if(symbol!=null&&symbol.getSymbol() == Symbol.SEMI_SY){
                getNextSymbol();
            }else {
                //语句之间漏分号
                errors.add(new Error(10,tokenizer.getLineIndex()));
            }
            statement(level,follow);
        }
        if(symbol!=null&&symbol.getSymbol() == Symbol.UNTIL_SY){
            getNextSymbol();
            conditionStatement(level, follow);
            interpreter.gen(PCode.JPC,0,cx);
        }else {
            //漏掉了until
            errors.add(new Error(33,tokenizer.getLineIndex()));
        }
    }

    /**
     * 解析<复合语句>;
     * <复合语句> ::= begin<语句>{;<语句>}end
     * @param level 所在层次
     * @param follow FOLLOW集
     */
    public void beginStatement(int level,HashSet<Integer> follow){
//        System.out.println("begin");
        getNextSymbol();
        HashSet<Integer> next = new HashSet<>(follow);
        //将分号和end添加到follow集中
        next.add(Symbol.SEMI_SY);
        next.add(Symbol.END_SY);
        //解析begin后面的<语句>
        statement(level,next);

        //解析{;<语句>}部分
        while(symbol!=null&&(FirstSet.STAT_FIRST.contains(symbol.getSymbol())
                || symbol.getSymbol() == Symbol.SEMI_SY)){
            if(symbol.getSymbol() == Symbol.SEMI_SY){
                getNextSymbol();
            }else {
                //语句之间漏分号
                errors.add(new Error(10,tokenizer.getLineIndex()));
            }
            //<语句>
            statement(level,next);
        }
        if(symbol!=null&&symbol.getSymbol() == Symbol.END_SY){
            getNextSymbol();
        }else {
            //应为分号或end
            errors.add(new Error(17,tokenizer.getLineIndex()));
            getNextSymbol();
        }
    }


    /**
     * 解析<条件语句>
     * <条件语句> ::= if<条件>then<语句>[else<语句>]
     * @param level 所在层次
     * @param follow FOLLOW集
     */
    public void ifStatement(int level ,HashSet<Integer> follow){
//        System.out.println("if");
        getNextSymbol();
        HashSet<Integer> next = new HashSet<>(follow);
        next.add(Symbol.THEN_SY);
        next.add(Symbol.DO_SY);
        //解析<条件>
        conditionStatement(level,next);
        if(symbol!=null&&symbol.getSymbol() == Symbol.THEN_SY){
            getNextSymbol();
        }else {
            //应为then
            errors.add(new Error(16,tokenizer.getLineIndex()));
            getNextSymbol();
        }

        //保存pcode指针地址
        int cx1 = interpreter.getPcodeIndex();
        //生成指令JPC 0,0
        interpreter.gen(PCode.JPC,0,0);
        //解析then后面的<语句>
        statement(level,follow);
        interpreter.getPcodes()[cx1].setA(interpreter.getPcodeIndex());

        //解析[else<语句>]
        if(symbol!=null&&symbol.getSymbol() == Symbol.ELSE_SY){
            interpreter.getPcodes()[cx1].setA(
                    interpreter.getPcodes()[cx1].getA()+1
            );
            getNextSymbol();
            int cx0 = interpreter.getPcodeIndex();
            interpreter.gen(PCode.JMP,0,0);
            statement(level, follow);
            interpreter.getPcodes()[cx0].setA(interpreter.getPcodeIndex());
        }
    }

    /**
     * 解析<调用语句>
     * <过程调用语句> ::= call<标识符>
     * @param level 所在层次
     * @param follow follow集
     */
    public void callStatement(int level,HashSet<Integer> follow){
//        System.out.println("call");
        getNextSymbol();
        if(symbol!=null&&symbol.getSymbol() == Symbol.ID_SY){
            int pos = table.position(symbol.getName());
            if(pos > 0){
                SymbolTableItem item = table.get(pos);
                //call 后面接的是过程
                if(item.getType() == 2){
                    //调用过程
                    interpreter.gen(PCode.CAL,level-item.getLevel(),item.getAddress());
                }else {
                    //不可调用常量或者变量
                    errors.add(new Error(15,tokenizer.getLineIndex()));
                }
            }else {
                //标识符未说明
                errors .add(new Error(11,tokenizer.getLineIndex()));
            }
            getNextSymbol();
        }else {
            //call 后面应为标识符
            errors.add(new Error(14,tokenizer.getLineIndex()));
            isSemi();
        }
    }


    /**
     * 解析<写语句>;
     * <写语句> ::= write'('<标识符>{,<标识符>}')'
     * @param level
     * @param follow
     */
    public void writeStatement(int level,HashSet<Integer> follow){
//        System.out.println("write");
        getNextSymbol();
        int flag = 1;
        if(symbol!=null){
            if(symbol.getSymbol() != Symbol.LEFT_PAR_SY) {
                //应为左括号
                errors.add(new Error(40,tokenizer.getLineIndex()));
                flag = 0;
            }
            /**
             * 注意：就算左括号缺失的话，需要继续往下面执行语句。
             */
            do{
                if(flag == 1){
                    getNextSymbol();
                }else {
                    flag = 1;
                }
                System.out.println(symbol);
                HashSet<Integer> next = new HashSet<>(follow);
                next.add(Symbol.LEFT_PAR_SY);
                next.add(Symbol.COMMA_SY);
                expression(level,next);
                //输出栈顶的值
                interpreter.gen(PCode.OPR,0,14);
            }while (symbol!=null&&symbol.getSymbol() == Symbol.COMMA_SY);

            if(symbol!=null&&symbol.getSymbol() == Symbol.RIGHT_PAR_SY){
                getNextSymbol();
            }else {
                //漏右括号
                errors.add(new Error(22,tokenizer.getLineIndex()));
            }
        }
        //OPR 0，15表示输出换行
        interpreter.gen(PCode.OPR,0,15);
    }

    /**
     * 解析<读语句>
     * <读语句> ::= read'('<标识符>{,<标识符>}')'
     * @param level 所在层次
     * @param follow FOLLOW集
     */
    public void readStatement(int level,HashSet<Integer> follow){
//        System.out.println("read");
        getNextSymbol();
        int flag = 1;
        if(symbol!=null){
            if(symbol.getSymbol() != Symbol.LEFT_PAR_SY) {
                //应该为左括号
                errors.add(new Error(40,tokenizer.getLineIndex()));
                flag = 0;
            }
            do{
                if(flag == 1){
                    getNextSymbol();
                }else {
                    flag = 1;
                }
                //查符号表
                int index;
                if(symbol.getSymbol() == Symbol.ID_SY){
                    index = table.position(symbol.getName());
                }else {
                    index = 0;
                }
                if(index == 0){
                    //标识符未说明
                     errors.add(new Error(11,tokenizer.getLineIndex()));
                }else {
                    SymbolTableItem item = table.get(index);
                    if(item.getType() == 1){
                        //OPR 0,16表示读入一个数据
                        interpreter.gen(PCode.OPR,0,16);
                        //将读入的数据存入变量中
                        interpreter.gen(PCode.STO,level-item.getLevel(),item.getAddress());
                    }else {
                        //read语句括号括号中标识符不是变量
                        errors.add(new Error(31,tokenizer.getLineIndex()));
                    }
                }
                getNextSymbol();
            }while (symbol!=null&&symbol.getSymbol() == Symbol.COMMA_SY);
        }

        if(symbol!=null&&symbol.getSymbol() == Symbol.RIGHT_PAR_SY){
            //以右括号结束
            getNextSymbol();
        }else {
            //漏右括号
            errors.add(new Error(22,tokenizer.getLineIndex()));
//            while(symbol!=null&&!follow.contains(symbol.getSymbol())){
//                getNextSymbol();
//            }
        }
    }
}
