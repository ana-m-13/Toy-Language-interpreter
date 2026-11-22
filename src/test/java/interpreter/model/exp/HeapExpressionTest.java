package interpreter.model.exp;

import interpreter.exceptions.ExpressionException;
import interpreter.exceptions.MyException;
import interpreter.model.PrgState;
import interpreter.model.stmt.NewStmt;
import interpreter.model.stmt.NopStmt;
import interpreter.model.stmt.VarDeclStmt;
import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeapExpressionTest {
    private PrgState state;

    @BeforeEach
    void setUp() throws MyException {
        state = PrgState.of(new NopStmt());
        new VarDeclStmt("v", new RefType(new IntType())).execute(state);
    }

    @Test
    void readsTheValueAllocatedByNew() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        var exp = new ReadHeapExp(new VarExp("v"));

        assertEquals(new IntValue(20), exp.eval(state.getSymTable(), state.getHeap()));
        assertEquals("rH(v)", exp.toString());
    }

    @Test
    void nestedReadsFollowReferencesThroughTheHeap() throws MyException {
        new VarDeclStmt("a", new RefType(new RefType(new IntType()))).execute(state);
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        new NewStmt("a", new VarExp("v")).execute(state);
        var firstRead = new ReadHeapExp(new VarExp("a"));
        var nestedRead = new ReadHeapExp(firstRead);

        assertEquals(new RefValue(1, new IntType()), firstRead.eval(state.getSymTable(), state.getHeap()));
        assertEquals(new IntValue(20), nestedRead.eval(state.getSymTable(), state.getHeap()));
        assertEquals("rH(rH(a))", nestedRead.toString());
    }

    @Test
    void heapReadsCanBeUsedAsArithmeticOperands() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        var exp = new ArithExp(ArithOp.ADD, new ReadHeapExp(new VarExp("v")),
                new ValueExp(new IntValue(5)));

        assertEquals(new IntValue(25), exp.eval(state.getSymTable(), state.getHeap()));
    }

    @Test
    void nonReferenceExpressionThrows() {
        var exp = new ReadHeapExp(new ValueExp(new IntValue(20)));

        var error = assertThrows(ExpressionException.class,
                () -> exp.eval(state.getSymTable(), state.getHeap()));

        assertEquals("rH: 20 is not a reference", error.getMessage());
    }

    @Test
    void readingADefaultReferenceThrowsForAddressZero() {
        var exp = new ReadHeapExp(new VarExp("v"));

        var error = assertThrows(ExpressionException.class,
                () -> exp.eval(state.getSymTable(), state.getHeap()));

        assertEquals("rH: address 0 is not in the heap", error.getMessage());
    }

    @Test
    void missingHeapAddressThrowsAnExpressionException() {
        var exp = new ReadHeapExp(new ValueExp(new RefValue(99, new IntType())));

        var error = assertThrows(ExpressionException.class,
                () -> exp.eval(state.getSymTable(), state.getHeap()));

        assertEquals("rH: address 99 is not in the heap", error.getMessage());
    }

    @Test
    void nestedReadRejectsADanglingInnerReference() {
        int address = state.getHeap().allocate(new RefValue(99, new IntType()));
        var exp = new ReadHeapExp(new ReadHeapExp(
                new ValueExp(new RefValue(address, new RefType(new IntType())))));

        var error = assertThrows(ExpressionException.class,
                () -> exp.eval(state.getSymTable(), state.getHeap()));

        assertEquals("rH: address 99 is not in the heap", error.getMessage());
    }

    @Test
    void operandEvaluationErrorsPropagate() {
        var exp = new ReadHeapExp(new VarExp("missing"));

        var error = assertThrows(ExpressionException.class,
                () -> exp.eval(state.getSymTable(), state.getHeap()));

        assertEquals("variable missing is not defined", error.getMessage());
    }
}
