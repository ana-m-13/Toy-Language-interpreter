package interpreter.controller;

import interpreter.exceptions.ExpressionException;
import interpreter.model.PrgState;
import interpreter.model.exp.ReadHeapExp;
import interpreter.model.exp.ValueExp;
import interpreter.model.exp.VarExp;
import interpreter.model.stmt.CompStmt;
import interpreter.model.stmt.IStmt;
import interpreter.model.stmt.NewStmt;
import interpreter.model.stmt.NopStmt;
import interpreter.model.stmt.PrintStmt;
import interpreter.model.stmt.VarDeclStmt;
import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import interpreter.repository.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ControllerGarbageCollectorTest {
    @TempDir
    Path dir;

    @Test
    void controllerCollectsAndLogsBeforeAndAfterEveryStep() throws Exception {
        PrgState state = PrgState.of(new CompStmt(new NopStmt(), new NopStmt()));
        state.getHeap().allocate(new IntValue(20));
        state.getHeap().allocate(new IntValue(30));
        state.getSymTable().put("v", new RefValue(2, new IntType()));
        Path logPath = dir.resolve("collection.txt");
        var controller = new Controller(new Repository(state, logPath.toString()), false);

        controller.allStep();

        assertEquals(Map.of(2, new IntValue(30)), state.getHeap().getContent());
        String[] snapshots = Files.readString(logPath).split("ExeStack:\n");
        // Initial state, then before/after collection for CompStmt and both NopStmts.
        assertEquals(8, snapshots.length);
        assertTrue(snapshots[1].contains("1 --> 20\n"));
        assertTrue(snapshots[2].contains("1 --> 20\n"));
        for (int i = 3; i < snapshots.length; i++) {
            assertFalse(snapshots[i].contains("1 --> 20\n"));
            assertTrue(snapshots[i].contains("2 --> 30\n"));
        }
    }

    @Test
    void ex9FailsBecauseUnsafeCollectionDeletesTheIndirectReferenceTarget() throws Exception {
        IStmt program = sequence(
                new VarDeclStmt("v", new RefType(new IntType())),
                new NewStmt("v", new ValueExp(new IntValue(20))),
                new VarDeclStmt("a", new RefType(new RefType(new IntType()))),
                new NewStmt("a", new VarExp("v")),
                new NewStmt("v", new ValueExp(new IntValue(30))),
                new PrintStmt(new ReadHeapExp(new ReadHeapExp(new VarExp("a")))));
        PrgState state = PrgState.of(program);
        Path logPath = dir.resolve("unsafe-ex9.txt");
        var controller = new Controller(new Repository(state, logPath.toString()), false);

        var error = assertThrows(ExpressionException.class, controller::allStep);

        assertEquals("rH: address 1 is not in the heap", error.getMessage());
        assertTrue(state.getOut().toList().isEmpty());
        assertEquals(Map.of(2, new RefValue(1, new IntType()), 3, new IntValue(30)),
                state.getHeap().getContent());
        String log = Files.readString(logPath);
        assertTrue(log.contains("Heap:\n1 --> 20\n2 --> (1, int)\n3 --> 30\n"));
        assertTrue(log.contains("Heap:\n2 --> (1, int)\n3 --> 30\n"));
    }

    private static IStmt sequence(IStmt... statements) {
        IStmt result = statements[statements.length - 1];
        for (int i = statements.length - 2; i >= 0; i--) {
            result = new CompStmt(statements[i], result);
        }
        return result;
    }
}
