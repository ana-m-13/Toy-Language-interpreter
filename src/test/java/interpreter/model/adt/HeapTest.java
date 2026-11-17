package interpreter.model.adt;

import interpreter.exceptions.ADTException;
import interpreter.model.value.IValue;
import interpreter.model.value.IntValue;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HeapTest {
    @Test
    void allocationsUseConsecutiveAddressesStartingAtOne() throws ADTException {
        IHeap heap = new MyHeap();

        assertEquals(1, heap.allocate(new IntValue(20)));
        assertEquals(2, heap.allocate(new IntValue(30)));
        assertEquals(3, heap.allocate(new IntValue(40)));
        assertEquals(new IntValue(20), heap.get(1));
        assertEquals(new IntValue(30), heap.get(2));
        assertEquals(new IntValue(40), heap.get(3));
        assertTrue(heap.isDefined(1));
        assertFalse(heap.isDefined(0));
    }

    @Test
    void updateChangesAnExistingLocation() throws ADTException {
        IHeap heap = new MyHeap();
        int address = heap.allocate(new IntValue(20));

        heap.update(address, new IntValue(30));

        assertEquals(new IntValue(30), heap.get(address));
        assertEquals(1, heap.getContent().size());
    }

    @Test
    void missingAddressesThrowWithoutAddingLocations() {
        IHeap heap = new MyHeap();
        heap.allocate(new IntValue(20));

        for (int address : new int[]{0, -1, 2}) {
            assertFalse(heap.isDefined(address));
            assertThrows(ADTException.class, () -> heap.get(address));
            assertThrows(ADTException.class, () -> heap.update(address, new IntValue(30)));
        }
        assertEquals(Map.of(1, new IntValue(20)), heap.getContent());
    }

    @Test
    void contentIsAnImmutableSnapshot() throws ADTException {
        IHeap heap = new MyHeap();
        heap.allocate(new IntValue(20));
        Map<Integer, IValue> snapshot = heap.getContent();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put(2, new IntValue(30)));
        heap.update(1, new IntValue(40));
        assertEquals(new IntValue(20), snapshot.get(1));
    }

    @Test
    void replacingContentCopiesTheMapAndDoesNotReuseCollectedAddresses() {
        IHeap heap = new MyHeap();
        heap.allocate(new IntValue(20));
        heap.allocate(new IntValue(30));
        var kept = new HashMap<Integer, IValue>();
        kept.put(1, new IntValue(20));

        heap.setContent(kept);
        kept.clear();

        assertEquals(Map.of(1, new IntValue(20)), heap.getContent());
        assertFalse(heap.isDefined(2));
        assertEquals(3, heap.allocate(new IntValue(40)));
        heap.setContent(Map.of());
        assertEquals(4, heap.allocate(new IntValue(50)));
    }
}
