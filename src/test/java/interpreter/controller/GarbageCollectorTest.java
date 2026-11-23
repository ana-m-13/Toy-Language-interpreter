package interpreter.controller;

import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.BoolValue;
import interpreter.model.value.IValue;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GarbageCollectorTest {
    @Test
    void rootsIncludeOnlyReferenceAddressesWithoutDuplicates() {
        List<IValue> values = List.of(new IntValue(7), new BoolValue(true),
                new RefValue(1, new IntType()), new RefValue(1, new IntType()),
                new RefValue(2, new RefType(new IntType())), new RefValue(0, new IntType()));

        assertEquals(Set.of(0, 1, 2), GarbageCollector.getAddrFromSymTable(values));
        assertTrue(GarbageCollector.getAddrFromSymTable(List.of()).isEmpty());
    }

    @Test
    void unsafeCollectorKeepsDirectlyReferencedLocationsAndLeavesTheInputUnchanged() {
        Map<Integer, IValue> heap = Map.of(1, new IntValue(20), 2, new IntValue(30));

        var kept = GarbageCollector.unsafeGarbageCollector(Set.of(0, 1, 99), heap);

        assertEquals(Map.of(1, new IntValue(20)), kept);
        assertEquals(Map.of(1, new IntValue(20), 2, new IntValue(30)), heap);
    }

    @Test
    void unsafeCollectorRemovesEverythingWhenThereAreNoRoots() {
        assertTrue(GarbageCollector.unsafeGarbageCollector(Set.of(),
                Map.of(1, new IntValue(20))).isEmpty());
        assertTrue(GarbageCollector.unsafeGarbageCollector(Set.of(1), Map.of()).isEmpty());
    }

    @Test
    void unsafeCollectorIncorrectlyRemovesAnIndirectlyReachableLocation() {
        // Ex9 after new(v, 30): v refers to 3, a refers to 2, and 2 still refers to 1.
        Map<Integer, IValue> heap = Map.of(1, new IntValue(20),
                2, new RefValue(1, new IntType()), 3, new IntValue(30));

        var kept = GarbageCollector.unsafeGarbageCollector(Set.of(2, 3), heap);

        assertEquals(Set.of(2, 3), kept.keySet());
        assertEquals(new RefValue(1, new IntType()), kept.get(2));
        assertFalse(kept.containsKey(1));
    }
}
