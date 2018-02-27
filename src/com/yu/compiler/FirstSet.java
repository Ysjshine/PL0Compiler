package com.yu.compiler;

import java.util.Arrays;
import java.util.HashSet;

/**
 * 头符号集
 */
public class FirstSet {
    public static final HashSet<Integer> DECLARE_FIRST
            = new HashSet<>(Arrays.asList(Symbol.CONST_SY,Symbol.VAR_SY,Symbol.PROCEDURE_SY));


    public static final HashSet<Integer> STAT_FIRST
            = new HashSet<>(
                    Arrays.asList(Symbol.CALL_SY,Symbol.BEGIN_SY
                            ,Symbol.IF_SY,Symbol.WHILE_SY,Symbol.REPEAT_SY,Symbol.READ_SY,Symbol.WRITE_SY));

    public static final HashSet<Integer> FACTOR_FIRST
            = new HashSet<>(Arrays.asList(Symbol.ID_SY,Symbol.NUM_SY,Symbol.LEFT_PAR_SY));

}
