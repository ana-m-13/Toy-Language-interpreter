package interpreter.view;

import interpreter.model.exp.*;
import interpreter.model.stmt.*;
import interpreter.model.type.*;
import interpreter.model.value.*;

public class Examples {
    private static IStmt sequence(IStmt... statements) {
        IStmt result = statements[statements.length - 1];
        for (int i = statements.length - 2; i >= 0; i--) {
            result = new CompStmt(statements[i], result);
        }
        return result;
    }

    // Ex6: Ref int v; new(v, 20); Ref Ref int a; new(a, v); print(v); print(a)
    public static final IStmt EX6 = sequence(
            new VarDeclStmt("v", new RefType(new IntType())),
            new NewStmt("v", new ValueExp(new IntValue(20))),
            new VarDeclStmt("a", new RefType(new RefType(new IntType()))),
            new NewStmt("a", new VarExp("v")),
            new PrintStmt(new VarExp("v")),
            new PrintStmt(new VarExp("a")));

    // Ex7: Ref int v; new(v, 20); Ref Ref int a; new(a, v); print(rH(v)); print(rH(rH(a)) + 5)
    public static final IStmt EX7 = sequence(
            new VarDeclStmt("v", new RefType(new IntType())),
            new NewStmt("v", new ValueExp(new IntValue(20))),
            new VarDeclStmt("a", new RefType(new RefType(new IntType()))),
            new NewStmt("a", new VarExp("v")),
            new PrintStmt(new ReadHeapExp(new VarExp("v"))),
            new PrintStmt(new ArithExp(ArithOp.ADD,
                    new ReadHeapExp(new ReadHeapExp(new VarExp("a"))), new ValueExp(new IntValue(5)))));

    // Ex8: Ref int v; new(v, 20); print(rH(v)); wH(v, 30); print(rH(v) + 5)
    public static final IStmt EX8 = sequence(
            new VarDeclStmt("v", new RefType(new IntType())),
            new NewStmt("v", new ValueExp(new IntValue(20))),
            new PrintStmt(new ReadHeapExp(new VarExp("v"))),
            new WriteHeapStmt("v", new ValueExp(new IntValue(30))),
            new PrintStmt(new ArithExp(ArithOp.ADD,
                    new ReadHeapExp(new VarExp("v")), new ValueExp(new IntValue(5)))));

    // Ex9: Ref int v; new(v, 20); Ref Ref int a; new(a, v); new(v, 30); print(rH(rH(a)))
    public static final IStmt EX9 = sequence(
            new VarDeclStmt("v", new RefType(new IntType())),
            new NewStmt("v", new ValueExp(new IntValue(20))),
            new VarDeclStmt("a", new RefType(new RefType(new IntType()))),
            new NewStmt("a", new VarExp("v")),
            new NewStmt("v", new ValueExp(new IntValue(30))),
            new PrintStmt(new ReadHeapExp(new ReadHeapExp(new VarExp("a")))));

    // Ex10: int v; v = 4; while (v > 0) (print(v); v = v - 1); print(v)
    public static final IStmt EX10 = sequence(
            new VarDeclStmt("v", new IntType()),
            new AssignStmt("v", new ValueExp(new IntValue(4))),
            new WhileStmt(new RelationalExp(Relop.GT, new VarExp("v"), new ValueExp(new IntValue(0))),
                    new CompStmt(new PrintStmt(new VarExp("v")),
                            new AssignStmt("v", new ArithExp(ArithOp.SUB,
                                    new VarExp("v"), new ValueExp(new IntValue(1)))))),
            new PrintStmt(new VarExp("v")));

    // Ex1: int v; v = 2; print(v)
    public static final IStmt EX1 = new CompStmt(
            new VarDeclStmt("v", new IntType()),
            new CompStmt(
                    new AssignStmt("v", new ValueExp(new IntValue(2))),
                    new PrintStmt(new VarExp("v"))
            )
    );

    // Ex2: int a; int b; a = 2 + 3 * 5; b = a + 1; print(b)
    public static final IStmt EX2 = new CompStmt(
            new VarDeclStmt("a", new IntType()),
            new CompStmt(
                    new VarDeclStmt("b", new IntType()),
                    new CompStmt(
                            new AssignStmt("a", new ArithExp(ArithOp.ADD,
                                    new ValueExp(new IntValue(2)),
                                    new ArithExp(ArithOp.MUL,
                                            new ValueExp(new IntValue(3)),
                                            new ValueExp(new IntValue(5))))),
                            new CompStmt(
                                    new AssignStmt("b", new ArithExp(ArithOp.ADD,
                                            new VarExp("a"),
                                            new ValueExp(new IntValue(1)))),
                                    new PrintStmt(new VarExp("b"))
                            )
                    )
            )
    );

    // Ex3: bool a; int v; a = true; (if a then v = 2 else v = 3); print(v)
    public static final IStmt EX3 = new CompStmt(
            new VarDeclStmt("a", new BoolType()),
            new CompStmt(
                    new VarDeclStmt("v", new IntType()),
                    new CompStmt(
                            new AssignStmt("a", new ValueExp(new BoolValue(true))),
                            new CompStmt(
                                    new IfStmt(new VarExp("a"),
                                            new AssignStmt("v", new ValueExp(new IntValue(2))),
                                            new AssignStmt("v", new ValueExp(new IntValue(3)))),
                                    new PrintStmt(new VarExp("v"))
                            )
                    )
            )
    );

    public static IStmt fileExample(String fileName) {
        return new CompStmt(new VarDeclStmt("varf", new StringType()),
                new CompStmt(new AssignStmt("varf", new ValueExp(new StringValue(fileName))),
                        new CompStmt(new OpenRFileStmt(new VarExp("varf")),
                                new CompStmt(new VarDeclStmt("varc", new IntType()),
                                        new CompStmt(new ReadFileStmt(new VarExp("varf"), "varc"),
                                                new CompStmt(new PrintStmt(new VarExp("varc")),
                                                        new CompStmt(new ReadFileStmt(new VarExp("varf"), "varc"),
                                                                new CompStmt(new PrintStmt(new VarExp("varc")),
                                                                        new CloseRFileStmt(new VarExp("varf"))))))))));
    }

    // Ex4: string varf; varf = "test.in"; openRFile(varf); int varc; readFile(varf, varc); print(varc); readFile(varf, varc); print(varc); closeRFile(varf)
    public static final IStmt EX4 = fileExample("test.in");

    // Ex5: int a; int b; a = 5; b = 7; if a < b then print(a) else print(b)
    public static final IStmt EX5 = new CompStmt(new VarDeclStmt("a", new IntType()),
            new CompStmt(new VarDeclStmt("b", new IntType()),
                    new CompStmt(new AssignStmt("a", new ValueExp(new IntValue(5))),
                            new CompStmt(new AssignStmt("b", new ValueExp(new IntValue(7))),
                                    new IfStmt(new RelationalExp(Relop.LT, new VarExp("a"), new VarExp("b")),
                                            new PrintStmt(new VarExp("a")),
                                            new PrintStmt(new VarExp("b")))))));
}
