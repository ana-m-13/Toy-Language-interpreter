package interpreter.model.type;
import interpreter.model.value.IntValue;
import interpreter.model.value.IValue;

public record IntType() implements IType {
    @Override public IValue defaultValue() { return new IntValue(0); }
    @Override public String toString() { return "int"; }
}