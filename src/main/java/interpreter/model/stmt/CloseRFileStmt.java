package interpreter.model.stmt;

import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.IExp;
import interpreter.model.value.StringValue;
import java.io.BufferedReader;
import java.io.IOException;

public record CloseRFileStmt(IExp exp) implements IStmt {
    @Override
    public PrgState execute(PrgState state) throws MyException {
        if (!(exp.eval(state.getSymTable()) instanceof StringValue(String fileName)))
            throw new StatementException("closeRFile: " + exp + " is not a string");

        var fileTable = state.getFileTable();
        if (!fileTable.isDefined(new StringValue(fileName)))
            throw new StatementException("closeRFile: file " + fileName + " is not open");

        try {
            BufferedReader reader = fileTable.lookup(new StringValue(fileName));
            reader.close();
            fileTable.remove(new StringValue(fileName));
        } catch (IOException e) {
            throw new StatementException("closeRFile: IOException closing " + fileName + ": " + e.getMessage());
        }
        return state;
    }

    @Override public String toString() { return "closeRFile(" + exp + ")"; }
}