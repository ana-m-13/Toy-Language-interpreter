package interpreter.model.stmt;

import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.ValueExp;
import interpreter.model.type.BoolType;
import interpreter.model.type.IntType;
import interpreter.model.value.BoolValue;
import interpreter.model.value.IntValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatementTest {
    private PrgState state;

    @BeforeEach
    void setUp() throws MyException {
        state = PrgState.of(new NopStmt());
        state.getExeStack().pop();
    }

    @Test
    void varDeclAndRedeclarionThrows() throws MyException {
        IStmt decl = new VarDeclStmt("x", new IntType());
        decl.execute(state);
        assertEquals(new IntValue(0), state.getSymTable().lookup("x"));
        assertThrows(StatementException.class, () -> decl.execute(state));
    }

    @Test
    void assignValidUndeclaredAndTypeMismatch() throws MyException {
        new VarDeclStmt("x", new IntType()).execute(state);
        new AssignStmt("x", new ValueExp(new IntValue(5))).execute(state);
        assertEquals(new IntValue(5), state.getSymTable().lookup("x"));
        assertThrows(StatementException.class,
                () -> new AssignStmt("y", new ValueExp(new IntValue(1))).execute(state));
        assertThrows(StatementException.class,
                () -> new AssignStmt("x", new ValueExp(new BoolValue(true))).execute(state));
    }

    @Test
    void ifStmtBranchesAndNonBoolConditionThrows() throws MyException {
        IStmt thenBranch = new PrintStmt(new ValueExp(new IntValue(1)));
        IStmt elseBranch = new PrintStmt(new ValueExp(new IntValue(2)));
        new IfStmt(new ValueExp(new BoolValue(true)), thenBranch, elseBranch).execute(state);
        assertEquals(thenBranch, state.getExeStack().pop());
        assertThrows(StatementException.class,
                () -> new IfStmt(new ValueExp(new IntValue(1)), thenBranch, elseBranch).execute(state));
    }
}