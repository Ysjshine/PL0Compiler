package com.yu.compiler;

import java.util.ArrayList;

public class SymbolTable {
    public static final int TABLE_MAX_SIZE = 1000;
    public static final int NEST_MAX_SIZE = 3;
    public static final int MAX_NUM = 1000000000;
    /**
     * 有效的符号表大小
     */
    private int tableIndex = 0;

    public int getTableIndex() {
        return tableIndex;
    }

    public void setTableIndex(int tableIndex) {
        this.tableIndex = tableIndex;
    }

    //符号表
    private SymbolTableItem[] tables = new SymbolTableItem[TABLE_MAX_SIZE];

    public SymbolTableItem get(int i){
        if(tables[i] == null){
            tables[i] = new SymbolTableItem("");
        }
        return tables[i];
    }
    /**
     * enter方法是将符号登入符号表中
     * @param symbol 要登入的符号
     * @param type 符号的类型，常量，变量，以及过程,在程序中分别用0，1，2表示
     * @param level 变量和过程所在的层次
     * @param address 变量的地址
     * @return 符号表溢出则返回false，否则返回true
     */
    public boolean enter(Symbol symbol,int type,int level,int address){
        tableIndex++;
        SymbolTableItem item = get(tableIndex);
        item.setName(symbol.getName());
        item.setType(type);
        if(type == 0){//constant
            item.setValue(symbol.getValue());
        }else if(type == 1){//variable
            item.setLevel(level);
            item.setAddress(address);
        }else if(type == 2){//procedure
            item.setLevel(level);
        }
        if(tableIndex > TABLE_MAX_SIZE) return false;
        return true;
    }

    /**
     * position方法主要是根据标识符名称查找符号表，并返回其在符号表中的位置。
     * @param name 标识符名称
     * @return 返回该标识符在符号表中的索引，如果没找到，返回-1.
     */
    public int position(String name){
        for(int i = tableIndex;i>0;i--){
            if(tables[i].getName().equals(name)){
                return i;
            }
        }
        return 0;
    }
    public void printTable(){
        for(SymbolTableItem item:tables){
            System.out.println(item);
        }
    }
}
