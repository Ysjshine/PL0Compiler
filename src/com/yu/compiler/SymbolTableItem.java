package com.yu.compiler;

public class SymbolTableItem {
    /**
     * name: 变量名
     * value：常量的值
     * type:类型（变量，常量，过程）
     * level：所在层次
     * address：地址(变量，过程)
     * size:大小
     */
    private String name;
    private int value;
    private int type;
    private int level;
    private int address;
    private int size;


    public SymbolTableItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "sym:"+name+" "+type+"\n";
    }
}
