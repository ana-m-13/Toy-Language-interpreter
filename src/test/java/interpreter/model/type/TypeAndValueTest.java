package interpreter.model.type;

import interpreter.model.value.BoolValue;
import interpreter.model.value.IntValue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypeAndValueTest {
    @Test
    void defaultValuesAndTypeEquality() {
        IType intType = new IntType();
        IType boolType = new BoolType();

        assertEquals(new IntValue(0), intType.defaultValue());
        assertEquals(new BoolValue(false), boolType.defaultValue());
        assertEquals(new IntType(), intType.defaultValue().getType());
        assertEquals(new BoolType(), boolType.defaultValue().getType());
        assertNotEquals(intType, boolType);
    }
}