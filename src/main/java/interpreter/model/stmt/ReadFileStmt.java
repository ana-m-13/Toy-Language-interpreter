package interpreter.model.stmt;

import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.IExp;
import interpreter.model.type.IntType;
import interpreter.model.value.IntValue;
import interpreter.model.value.StringValue;
import java.io.BufferedReader;
import java.io.IOException;

public record ReadFileStmt(IExp exp, String varName) implements IStmt {
    @Override
    public PrgState execute(PrgState state) throws MyException {
        var symTable = state.getSymTable();
        if (!symTable.isDefined(varName))
            throw new StatementException("readFile: variable " + varName + " is not declared");
        if (!symTable.lookup(varName).getType().equals(new IntType()))
            throw new StatementException("readFile: variable " + varName + " is not of type int");

        if (!(exp.eval(symTable) instanceof StringValue(String fileName)))
            throw new StatementException("readFile: " + exp + " is not a string");

        var fileTable = state.getFileTable();
        if (!fileTable.isDefined(new StringValue(fileName)))
            throw new StatementException("readFile: file " + fileName + " is not open");

        try {
            BufferedReader reader = fileTable.lookup(new StringValue(fileName));
            String line = reader.readLine();
            int value = (line == null) ? 0 : Integer.parseInt(line.strip());
            symTable.update(varName, new IntValue(value));
        } catch (IOException e) {
            throw new StatementException("readFile: IOException reading " + fileName + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new StatementException("readFile: non-integer line in " + fileName);
        }
        return state;
    }

    @Override public String toString() { return "readFile(" + exp + ", " + varName + ")"; }
}