package interpreter.controller;

import interpreter.exceptions.*;
import interpreter.model.PrgState;
import interpreter.model.exp.*;
import interpreter.model.stmt.*;
import interpreter.model.type.*;
import interpreter.model.value.*;
import interpreter.repository.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class Seminar4ErrorTest {
    @TempDir
    Path dir;

    private PrgState run(IStmt program) throws MyException {
        PrgState state = PrgState.of(program);
        var repo = new Repository(state, dir.resolve("error_log.txt").toString());
        new Controller(repo, false).allStep();
        return state;
    }

    @Test
    void divisionByZeroThrows() {
        IStmt prg = new CompStmt(
                new VarDeclStmt("x", new IntType()),
                new CompStmt(
                        new AssignStmt("x", new ArithExp(ArithOp.DIV,
                                new ValueExp(new IntValue(10)),
                                new ArithExp(ArithOp.SUB,
                                        new ValueExp(new IntValue(2)),
                                        new ValueExp(new IntValue(2))))),
                        new PrintStmt(new VarExp("x"))
                )
        );
        var ex = assertThrows(ExpressionException.class, () -> run(prg));
        assertEquals("division by zero", ex.getMessage());
    }

    @Test
    void redeclaredVariableThrows() {
        IStmt prg = new CompStmt(
                new VarDeclStmt("x", new IntType()),
                new VarDeclStmt("x", new BoolType())
        );
        var ex = assertThrows(StatementException.class, () -> run(prg));
        assertEquals("variable x is already declared", ex.getMessage());
    }

    @Test
    void typeMismatchInAssignmentThrows() {
        IStmt prg = new CompStmt(
                new VarDeclStmt("x", new IntType()),
                new AssignStmt("x", new ValueExp(new BoolValue(true)))
        );
        var ex = assertThrows(StatementException.class, () -> run(prg));
        assertEquals("declared type of variable x and type of the assigned expression do not match", ex.getMessage());
    }

    @Test
    void nonBooleanIfConditionThrows() {
        IStmt prg = new CompStmt(
                new VarDeclStmt("x", new IntType()),
                new IfStmt(
                        new VarExp("x"),
                        new PrintStmt(new ValueExp(new IntValue(1))),
                        new PrintStmt(new ValueExp(new IntValue(0)))
                )
        );
        var ex = assertThrows(StatementException.class, () -> run(prg));
        assertEquals("conditional expression is not a boolean", ex.getMessage());
    }

    @Test
    void undeclaredVariableAssignmentThrows() {
        IStmt prg = new CompStmt(
                new AssignStmt("y", new ValueExp(new IntValue(3))),
                new PrintStmt(new VarExp("y"))
        );
        var ex = assertThrows(StatementException.class, () -> run(prg));
        assertEquals("variable y was not declared", ex.getMessage());
    }

    @Test
    void nonBooleanOperandInLogicExpThrows() {
        IStmt prg = new CompStmt(
                new VarDeclStmt("b", new BoolType()),
                new AssignStmt("b", new LogicExp(LogicOp.AND,
                        new ValueExp(new IntValue(1)),
                        new ValueExp(new BoolValue(true))))
        );
        var ex = assertThrows(ExpressionException.class, () -> run(prg));
        assertEquals("first operand is not a boolean", ex.getMessage());
    }
}