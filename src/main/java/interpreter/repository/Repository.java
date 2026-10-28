package interpreter.repository;

import interpreter.exceptions.MyException;
import interpreter.model.PrgState;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Repository implements IRepository {
    private final List<PrgState> prgStates = new ArrayList<>();
    private final String logFilePath;

    public Repository(PrgState initialState, String logFilePath) {
        this.prgStates.add(initialState);
        this.logFilePath = logFilePath;
    }

    @Override public PrgState getCrtPrg() { return prgStates.getFirst(); }

    @Override
    public void logPrgStateExec() throws MyException {
        try (var logFile = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)))) {
            logFile.println(getCrtPrg().toLogString());
        } catch (IOException e) {
            throw new MyException("cannot write the log file " + logFilePath + ": " + e.getMessage());
        }
    }
}