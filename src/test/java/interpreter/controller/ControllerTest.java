package interpreter.controller;

import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.stmt.IStmt;
import interpreter.model.value.IntValue;
import interpreter.repository.Repository;
import interpreter.view.Examples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    @TempDir
    Path dir;

    private PrgState run(IStmt program) throws MyException {
        PrgState state = PrgState.of(program);
        var repo = new Repository(state, dir.resolve("log.txt").toString());
        new Controller(repo, false).allStep();
        return state;
    }

    @Test
    void example1PrintsTwo() throws MyException {
        PrgState state = run(Examples.EX1);
        assertTrue(state.getExeStack().isEmpty());
        assertEquals(new IntValue(2), state.getOut().get(0));
    }

    @Test
    void example2PrintsEighteen() throws MyException {
        PrgState state = run(Examples.EX2);
        assertTrue(state.getExeStack().isEmpty());
        assertEquals(new IntValue(18), state.getOut().get(0));
    }

    @Test
    void example3PrintsTwo() throws MyException {
        PrgState state = run(Examples.EX3);
        assertTrue(state.getExeStack().isEmpty());
        assertEquals(new IntValue(2), state.getOut().get(0));
    }

    @Test
    void fileExampleReadsBothNumbersAndClosesTheFile() throws Exception {
        Path in = dir.resolve("test.in");
        Files.writeString(in, "15\n50\n");
        PrgState s = run(Examples.fileExample(in.toString()));
        assertEquals(new IntValue(15), s.getOut().get(0));
        assertEquals(new IntValue(50), s.getOut().get(1));
        assertTrue(s.getFileTable().toMap().isEmpty());
    }

    @Test
    void example5PrintsFive() throws MyException {
        PrgState state = run(Examples.EX5);
        assertTrue(state.getExeStack().isEmpty());
        assertEquals(new IntValue(5), state.getOut().get(0));
    }

    @Test
    void logFileContainsTheFiveSectionsInOrder() throws Exception {
        run(Examples.EX1);
        String log = Files.readString(dir.resolve("log.txt"));
        int a = log.indexOf("ExeStack:");
        int b = log.indexOf("SymTable:");
        int c = log.indexOf("Out:");
        int d = log.indexOf("FileTable:");
        int e = log.indexOf("Heap:");
        assertTrue(0 <= a && a < b && b < c && c < d && d < e);
    }

    @Test
    void heapIsLoggedInNumericAddressOrder() throws Exception {
        PrgState state = PrgState.of(Examples.EX1);
        assertTrue(state.getHeap().getContent().isEmpty());
        for (int value = 1; value <= 12; value++) {
            state.getHeap().allocate(new IntValue(value * 10));
        }
        var repo = new Repository(state, dir.resolve("heap-log.txt").toString());
        repo.logPrgStateExec();

        String log = Files.readString(dir.resolve("heap-log.txt"));
        var expectedHeap = new StringBuilder("Heap:\n");
        for (int address = 1; address <= 12; address++) {
            expectedHeap.append(address).append(" --> ").append(address * 10).append('\n');
        }
        assertTrue(log.endsWith(expectedHeap + System.lineSeparator()));
        assertTrue(state.toString().contains("Heap: " + state.getHeap()));
    }

    @Test
    void programStatesHaveIndependentHeaps() {
        PrgState first = PrgState.of(Examples.EX1);
        PrgState second = PrgState.of(Examples.EX1);

        first.getHeap().allocate(new IntValue(20));

        assertTrue(second.getHeap().getContent().isEmpty());
        assertEquals(1, second.getHeap().allocate(new IntValue(30)));
    }

    @Test
    void oneStepOnEmptyStackThrows() throws MyException {
        PrgState state = PrgState.of(Examples.EX1);
        state.getExeStack().pop();
        var repo = new Repository(state, dir.resolve("log.txt").toString());
        Controller ctrl = new Controller(repo, false);
        assertThrows(StatementException.class, () -> ctrl.oneStep(state));
    }
}
