package interpreter.model.stmt;

import interpreter.exceptions.ExpressionException;
import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.ArithExp;
import interpreter.model.exp.ArithOp;
import interpreter.model.exp.ReadHeapExp;
import interpreter.model.exp.ValueExp;
import interpreter.model.exp.VarExp;
import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.BoolValue;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HeapStmtTest {
    private PrgState state;

    @BeforeEach
    void setUp() throws MyException {
        state = PrgState.of(new NopStmt());
        new VarDeclStmt("v", new RefType(new IntType())).execute(state);
    }

    @Test
    void writeUpdatesTheSharedLocationWithoutChangingReferences() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        new VarDeclStmt("alias", new RefType(new IntType())).execute(state);
        new AssignStmt("alias", new VarExp("v")).execute(state);
        var stmt = new WriteHeapStmt("v", new ArithExp(ArithOp.ADD,
                new ReadHeapExp(new VarExp("v")), new ValueExp(new IntValue(10))));

        assertSame(state, stmt.execute(state));

        assertEquals(Map.of(1, new IntValue(30)), state.getHeap().getContent());
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("v"));
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("alias"));
        assertEquals(new IntValue(30),
                new ReadHeapExp(new VarExp("alias")).eval(state.getSymTable(), state.getHeap()));
        assertEquals("wH(v, (rH(v) + 10))", stmt.toString());
        assertEquals(2, state.getHeap().allocate(new IntValue(40)));
    }

    @Test
    void writeCanReplaceAStoredReferenceWithAMatchingReference() throws MyException {
        new VarDeclStmt("a", new RefType(new RefType(new IntType()))).execute(state);
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        new NewStmt("a", new VarExp("v")).execute(state);
        new NewStmt("v", new ValueExp(new IntValue(30))).execute(state);

        new WriteHeapStmt("a", new VarExp("v")).execute(state);

        assertEquals(new RefValue(3, new IntType()), state.getHeap().get(2));
        assertEquals(new RefValue(2, new RefType(new IntType())), state.getSymTable().lookup("a"));
        assertEquals(new IntValue(30), new ReadHeapExp(new ReadHeapExp(new VarExp("a")))
                .eval(state.getSymTable(), state.getHeap()));
    }

    @Test
    void writeWithAnUndeclaredVariableThrows() {
        var stmt = new WriteHeapStmt("missing", new ValueExp(new IntValue(30)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("wH: missing is not declared", error.getMessage());
        assertFalse(state.getSymTable().isDefined("missing"));
        assertTrue(state.getHeap().getContent().isEmpty());
    }

    @Test
    void writeWithANonReferenceVariableThrows() throws MyException {
        new VarDeclStmt("x", new IntType()).execute(state);
        var stmt = new WriteHeapStmt("x", new ValueExp(new IntValue(30)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("wH: x is not a reference", error.getMessage());
        assertEquals(new IntValue(0), state.getSymTable().lookup("x"));
        assertTrue(state.getHeap().getContent().isEmpty());
    }

    @Test
    void writeToNullOrMissingAddressThrowsWithoutAddingLocations() throws MyException {
        for (int address : new int[]{0, 99}) {
            state.getSymTable().update("v", new RefValue(address, new IntType()));
            var stmt = new WriteHeapStmt("v", new ValueExp(new IntValue(30)));

            var error = assertThrows(StatementException.class, () -> stmt.execute(state));

            assertEquals("wH: address " + address + " is not in the heap", error.getMessage());
            assertEquals(new RefValue(address, new IntType()), state.getSymTable().lookup("v"));
            assertTrue(state.getHeap().getContent().isEmpty());
        }
    }

    @Test
    void writeWithTheWrongTypePreservesTheHeapAndReference() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        var stmt = new WriteHeapStmt("v", new ValueExp(new BoolValue(true)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("wH: true has type bool, expected int", error.getMessage());
        assertEquals(Map.of(1, new IntValue(20)), state.getHeap().getContent());
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("v"));
    }

    @Test
    void writeRejectsReferencesWithTheWrongNestedType() throws MyException {
        new VarDeclStmt("a", new RefType(new RefType(new IntType()))).execute(state);
        new NewStmt("a", new VarExp("v")).execute(state);
        var before = state.getHeap().getContent();
        var stmt = new WriteHeapStmt("a", new ValueExp(new RefValue(0, new RefType(new IntType()))));

        assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals(before, state.getHeap().getContent());
    }

    @Test
    void writeExpressionFailurePreservesTheHeapAndReference() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        var stmt = new WriteHeapStmt("v", new VarExp("missing"));

        assertThrows(ExpressionException.class, () -> stmt.execute(state));

        assertEquals(Map.of(1, new IntValue(20)), state.getHeap().getContent());
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("v"));
    }

    @Test
    void newAllocatesAtAddressOneAndUpdatesTheVariable() throws MyException {
        var stmt = new NewStmt("v", new ValueExp(new IntValue(20)));

        assertSame(state, stmt.execute(state));
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("v"));
        assertEquals(new IntValue(20), state.getHeap().get(1));
        assertEquals("new(v, 20)", stmt.toString());
    }

    @Test
    void newAllocatesAFreshLocationWhenTheVariableAlreadyReferencesOne() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        new NewStmt("v", new ValueExp(new IntValue(30))).execute(state);

        assertEquals(new RefValue(2, new IntType()), state.getSymTable().lookup("v"));
        assertEquals(Map.of(1, new IntValue(20), 2, new IntValue(30)), state.getHeap().getContent());
    }

    @Test
    void newCanStoreANestedReference() throws MyException {
        new VarDeclStmt("a", new RefType(new RefType(new IntType()))).execute(state);
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        new NewStmt("a", new VarExp("v")).execute(state);

        assertEquals(new RefValue(2, new RefType(new IntType())), state.getSymTable().lookup("a"));
        assertEquals(new RefValue(1, new IntType()), state.getHeap().get(2));
        assertEquals(new IntValue(20), state.getHeap().get(1));
    }

    @Test
    void newWithAnUndeclaredVariableThrows() {
        var stmt = new NewStmt("missing", new ValueExp(new IntValue(20)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("new: missing is not declared", error.getMessage());
        assertTrue(state.getHeap().getContent().isEmpty());
        assertFalse(state.getSymTable().isDefined("missing"));
    }

    @Test
    void newWithANonReferenceVariableThrows() throws MyException {
        new VarDeclStmt("x", new IntType()).execute(state);
        var stmt = new NewStmt("x", new ValueExp(new IntValue(20)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("new: x is not a reference", error.getMessage());
        assertEquals(new IntValue(0), state.getSymTable().lookup("x"));
        assertTrue(state.getHeap().getContent().isEmpty());
    }

    @Test
    void newWithTheWrongTypePreservesExistingReferenceAndHeap() throws MyException {
        new NewStmt("v", new ValueExp(new IntValue(20))).execute(state);
        var stmt = new NewStmt("v", new ValueExp(new BoolValue(true)));

        var error = assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals("new: true has type bool, expected int", error.getMessage());
        assertEquals(new RefValue(1, new IntType()), state.getSymTable().lookup("v"));
        assertEquals(Map.of(1, new IntValue(20)), state.getHeap().getContent());
        assertEquals(2, state.getHeap().allocate(new IntValue(30)));
    }

    @Test
    void newRejectsReferencesWithTheWrongNestedType() throws MyException {
        new VarDeclStmt("a", new RefType(new RefType(new IntType()))).execute(state);
        var stmt = new NewStmt("a", new ValueExp(new RefValue(0, new RefType(new IntType()))));

        assertThrows(StatementException.class, () -> stmt.execute(state));

        assertEquals(new RefValue(0, new RefType(new IntType())), state.getSymTable().lookup("a"));
        assertTrue(state.getHeap().getContent().isEmpty());
    }

    @Test
    void expressionFailureDoesNotAllocateOrUpdateTheVariable() throws MyException {
        var stmt = new NewStmt("v", new VarExp("missing"));

        assertThrows(ExpressionException.class, () -> stmt.execute(state));

        assertEquals(new RefValue(0, new IntType()), state.getSymTable().lookup("v"));
        assertTrue(state.getHeap().getContent().isEmpty());
    }
}
