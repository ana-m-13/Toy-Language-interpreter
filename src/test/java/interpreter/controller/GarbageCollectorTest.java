package interpreter.controller;

import interpreter.model.type.IntType;
import interpreter.model.type.RefType;
import interpreter.model.value.BoolValue;
import interpreter.model.value.IValue;
import interpreter.model.value.IntValue;
import interpreter.model.value.RefValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GarbageCollectorTest {
    @Test
    void safeCollectorKeepsIndirectReferencesAndRemovesGarbage() {
        Map<Integer, IValue> heap = Map.of(1, new IntValue(20),
                2, new RefValue(1, new IntType()), 3, new IntValue(30), 4, new IntValue(7));

        assertEquals(Set.of(1, 2, 3), GarbageCollector.reachableAddresses(Set.of(2, 3), heap));
        assertEquals(Map.of(1, new IntValue(20), 2, new RefValue(1, new IntType()),
                3, new IntValue(30)), GarbageCollector.safeGarbageCollector(Set.of(2, 3), heap));
        assertEquals(4, heap.size());
    }

    @Test
    @Timeout(5)
    void reachableCyclesAreKeptAndUnreachableCyclesAreRemoved() {
        Map<Integer, IValue> heap = Map.of(1, new RefValue(2, new IntType()),
                2, new RefValue(1, new IntType()), 3, new RefValue(3, new IntType()),
                4, new RefValue(5, new IntType()), 5, new RefValue(4, new IntType()));

        assertEquals(Set.of(1, 2, 3), GarbageCollector.reachableAddresses(Set.of(1, 3), heap));
        assertEquals(Set.of(1, 2, 3), GarbageCollector.safeGarbageCollector(Set.of(1, 3), heap).keySet());
    }

    @Test
    void nullAndMissingAddressesAreNotAddedToTheReachableLocations() {
        Map<Integer, IValue> heap = Map.of(1, new RefValue(0, new IntType()),
                2, new RefValue(99, new IntType()));

        assertEquals(Set.of(1, 2), GarbageCollector.reachableAddresses(Set.of(0, 1, 2, 99), heap));
        assertEquals(heap, GarbageCollector.safeGarbageCollector(Set.of(1, 2), heap));
        assertTrue(GarbageCollector.safeGarbageCollector(Set.of(), heap).isEmpty());
        assertTrue(GarbageCollector.safeGarbageCollector(Set.of(1), Map.of()).isEmpty());
    }

    @Test
    @Timeout(5)
    void longReferenceChainsDoNotRequireRecursiveTraversal() {
        Map<Integer, IValue> heap = new HashMap<>();
        for (int address = 1; address < 10000; address++) {
            heap.put(address, new RefValue(address + 1, new IntType()));
        }
        heap.put(10000, new IntValue(20));

        assertEquals(heap.keySet(), GarbageCollector.reachableAddresses(Set.of(1), heap));
    }

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
