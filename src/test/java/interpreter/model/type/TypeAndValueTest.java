package interpreter.model.type;

import interpreter.model.value.BoolValue;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypeAndValueTest {
    @Test
    void referenceDefaultsPreserveTheLocationType() {
        var intRef = new RefType(new IntType());
        var nestedRef = new RefType(intRef);

        assertEquals(new RefValue(0, new IntType()), intRef.defaultValue());
        assertEquals(new RefValue(0, intRef), nestedRef.defaultValue());
        assertEquals(intRef, intRef.defaultValue().getType());
        assertEquals(nestedRef, nestedRef.defaultValue().getType());
    }

    @Test
    void referenceTypesCompareInnerTypesRecursively() {
        var nestedRef = new RefType(new RefType(new IntType()));

        assertEquals(new RefType(new RefType(new IntType())), nestedRef);
        assertNotEquals(new RefType(new IntType()), nestedRef);
        assertNotEquals(new RefType(new RefType(new BoolType())), nestedRef);
    }

    @Test
    void referenceValuesExposeTheirTypeAndReadableRepresentation() {
        var value = new RefValue(10, new RefType(new IntType()));

        assertEquals(new RefType(new RefType(new IntType())), value.getType());
        assertEquals("Ref(Ref(int))", value.getType().toString());
        assertEquals("(10, Ref(int))", value.toString());
    }

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
