package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.List;

public interface Expression {
    Value eval(ExecutionContext ctx, Host host);
}

record NumberLiteral(double v) implements Expression {
    @Override public Value eval(ExecutionContext ctx, Host host) { return Value.of(v); }
}

record StringLiteral(String v) implements Expression {
    @Override public Value eval(ExecutionContext ctx, Host host) { return Value.of(v); }
}

record VariableRef(String name) implements Expression {
    @Override public Value eval(ExecutionContext ctx, Host host) { return ctx.getVar(name); }
}

record UnaryOp(char op, Expression operand) implements Expression {
    @Override public Value eval(ExecutionContext ctx, Host host) {
        Value v = operand.eval(ctx, host);
        return switch (op) {
            case '-' -> Value.of(-v.asNumber());
            case '!' -> Value.of(v.asNumber() == 0 ? -1 : 0);
            default  -> v;
        };
    }
}

record BinaryOp(String op, Expression left, Expression right) implements Expression {

    @Override
    public Value eval(ExecutionContext ctx, Host host) {
        Value l = left.eval(ctx, host);
        Value r = right.eval(ctx, host);

        // String concatenation
        if (op.equals("+") && (l.isString() || r.isString())) {
            return Value.of(l.asString() + r.asString());
        }
        // String comparison
        if (l.isString() && r.isString()) {
            int cmp = l.asString().compareTo(r.asString());
            return Value.of(cmpResult(cmp));
        }

        double a = l.asNumber(), b = r.asNumber();
        return switch (op) {
            case "+"   -> Value.of(a + b);
            case "-"   -> Value.of(a - b);
            case "*"   -> Value.of(a * b);
            case "/"   -> b == 0 ? Value.of(0) : Value.of(a / b);
            case "\\"  -> b == 0 ? Value.of(0) : Value.of((long)(a / b));
            case "MOD" -> b == 0 ? Value.of(0) : Value.of(a % b);
            case "^"   -> Value.of(Math.pow(a, b));
            case "="   -> Value.of(a == b ? -1 : 0);
            case "<>"  -> Value.of(a != b ? -1 : 0);
            case "<"   -> Value.of(a <  b ? -1 : 0);
            case ">"   -> Value.of(a >  b ? -1 : 0);
            case "<="  -> Value.of(a <= b ? -1 : 0);
            case ">="  -> Value.of(a >= b ? -1 : 0);
            case "AND" -> Value.of((a != 0 && b != 0) ? -1 : 0);
            case "OR"  -> Value.of((a != 0 || b != 0) ? -1 : 0);
            default    -> Value.of(0);
        };
    }

    private double cmpResult(int cmp) {
        return switch (op) {
            case "="  -> cmp == 0 ? -1 : 0;
            case "<>" -> cmp != 0 ? -1 : 0;
            case "<"  -> cmp <  0 ? -1 : 0;
            case ">"  -> cmp >  0 ? -1 : 0;
            case "<=" -> cmp <= 0 ? -1 : 0;
            case ">=" -> cmp >= 0 ? -1 : 0;
            default   -> 0;
        };
    }
}

record FunctionCall(String name, List<Expression> args) implements Expression {
    @Override
    public Value eval(ExecutionContext ctx, Host host) {
        List<Value> vals = new java.util.ArrayList<>(args.size());
        for (Expression e : args) vals.add(e.eval(ctx, host));
        return BuiltinFunctions.call(name, vals);
    }
}