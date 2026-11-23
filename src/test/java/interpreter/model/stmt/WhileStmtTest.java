package interpreter.model.stmt;

import interpreter.controller.Controller;
import interpreter.exceptions.ExpressionException;
import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.ArithExp;
import interpreter.model.exp.ArithOp;
import interpreter.model.exp.ReadHeapExp;
import interpreter.model.exp.RelationalExp;
import interpreter.model.exp.Relop;
import interpreter.model.exp.ValueExp;
import interpreter.model.exp.VarExp;
import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.BoolValue;
import interpreter.model.value.IntValue;
import interpreter.repository.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class WhileStmtTest {
    @TempDir
    Path dir;

    private PrgState runCountdown(int initialValue) throws MyException {
        var condition = new RelationalExp(Relop.GT, new VarExp("v"), new ValueExp(new IntValue(0)));
        var body = new CompStmt(new PrintStmt(new VarExp("v")),
                new AssignStmt("v", new ArithExp(ArithOp.SUB,
                        new VarExp("v"), new ValueExp(new IntValue(1)))));
        var program = new CompStmt(new VarDeclStmt("v", new IntType()),
                new CompStmt(new AssignStmt("v", new ValueExp(new IntValue(initialValue))),
                        new CompStmt(new WhileStmt(condition, body), new PrintStmt(new VarExp("v")))));
        PrgState state = PrgState.of(program);
        new Controller(new Repository(state, dir.resolve("log.txt").toString()), false).allStep();
        assertTrue(state.getExeStack().isEmpty());
        assertEquals(new IntValue(0), state.getSymTable().lookup("v"));
        return state;
    }

    @Test
    void falseConditionSkipsTheBodyAndContinuesAfterTheLoop() throws MyException {
        assertEquals(List.of(new IntValue(0)), runCountdown(0).getOut().toList());
    }

    @Test
    void loopRunsOnce() throws MyException {
        assertEquals(List.of(new IntValue(1), new IntValue(0)), runCountdown(1).getOut().toList());
    }

    @Test
    void loopReevaluatesItsConditionAfterEveryIteration() throws MyException {
        assertEquals(List.of(new IntValue(4), new IntValue(3), new IntValue(2),
                new IntValue(1), new IntValue(0)), runCountdown(4).getOut().toList());
    }

    @Test
    void trueConditionSchedulesTheBodyBeforeTheLoopAndRemainingStatements() throws MyException {
        IStmt following = new NopStmt();
        IStmt body = new PrintStmt(new ValueExp(new IntValue(1)));
        var loop = new WhileStmt(new ValueExp(new BoolValue(true)), body);
        PrgState state = PrgState.of(following);

        assertSame(state, loop.execute(state));

        assertSame(body, state.getExeStack().pop());
        assertSame(loop, state.getExeStack().pop());
        assertSame(following, state.getExeStack().pop());
        assertTrue(state.getOut().toList().isEmpty());
        assertEquals("(while (true) print(1))", loop.toString());
    }

    @Test
    void nonBooleanConditionThrowsWithoutSchedulingTheBody() throws MyException {
        IStmt following = new NopStmt();
        PrgState state = PrgState.of(following);
        var loop = new WhileStmt(new ValueExp(new IntValue(1)), new NopStmt());

        var error = assertThrows(StatementException.class, () -> loop.execute(state));

        assertEquals("while: condition 1 is not a boolean", error.getMessage());
        assertSame(following, state.getExeStack().pop());
        assertTrue(state.getExeStack().isEmpty());
    }

    @Test
    void conditionEvaluationErrorsPropagate() {
        PrgState state = PrgState.of(new NopStmt());
        var loop = new WhileStmt(new VarExp("missing"), new NopStmt());

        assertThrows(ExpressionException.class, () -> loop.execute(state));
    }

    @Test
    void conditionCanReadHeapValuesChangedByTheBody() throws MyException {
        var condition = new RelationalExp(Relop.GT, new ReadHeapExp(new VarExp("v")),
                new ValueExp(new IntValue(0)));
        var body = new WriteHeapStmt("v", new ArithExp(ArithOp.SUB,
                new ReadHeapExp(new VarExp("v")), new ValueExp(new IntValue(1))));
        var program = new CompStmt(new VarDeclStmt("v", new RefType(new IntType())),
                new CompStmt(new NewStmt("v", new ValueExp(new IntValue(3))),
                        new CompStmt(new WhileStmt(condition, body),
                                new PrintStmt(new ReadHeapExp(new VarExp("v"))))));
        PrgState state = PrgState.of(program);

        new Controller(new Repository(state, dir.resolve("heap-log.txt").toString()), false).allStep();

        assertEquals(List.of(new IntValue(0)), state.getOut().toList());
        assertEquals(new IntValue(0), state.getHeap().get(1));
        assertTrue(state.getExeStack().isEmpty());
    }
}
