package interpreter.model.stmt;
import interpreter.exceptions.MyException;
import interpreter.exceptions.StatementException;
import interpreter.model.PrgState;
import interpreter.model.exp.IExp;
import interpreter.model.value.BoolValue;

public record IfStmt(IExp cond, IStmt thenS, IStmt elseS) implements IStmt {
    @Override
    public PrgState execute(PrgState state) throws MyException {
        if (!(cond.eval(state.getSymTable()) instanceof BoolValue(boolean b)))
            throw new StatementException("conditional expression is not a boolean");
        state.getExeStack().push(b ? thenS : elseS);
        return state;
    }
    @Override
    public String toString() { return "(if " + cond + " then " + thenS + " else " + elseS + ")"; }
}