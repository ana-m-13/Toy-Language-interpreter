package interpreter.controller;

import interpreter.model.value.IValue;
import interpreter.model.value.RefValue;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class GarbageCollector {
    private GarbageCollector() {}

    /** Finds existing heap locations reachable from the symbol-table roots. */
    public static Set<Integer> reachableAddresses(Set<Integer> roots, Map<Integer, IValue> heap) {
        Set<Integer> reachable = new HashSet<>();
        var pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            int address = pending.removeFirst();
            if (!heap.containsKey(address) || !reachable.add(address)) {
                continue;
            }
            if (heap.get(address) instanceof RefValue ref) {
                pending.addLast(ref.address());
            }
        }
        return reachable;
    }

    public static Map<Integer, IValue> safeGarbageCollector(Set<Integer> roots,
                                                          Map<Integer, IValue> heap) {
        Set<Integer> reachable = reachableAddresses(roots, heap);
        return heap.entrySet().stream()
                .filter(entry -> reachable.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** The addresses referred to directly by reference values in the symbol table. */
    public static Set<Integer> getAddrFromSymTable(Collection<IValue> symTableValues) {
        return symTableValues.stream()
                .filter(value -> value instanceof RefValue)
                .map(value -> ((RefValue) value).address())
                .collect(Collectors.toSet());
    }

    /** UNSAFE: does not preserve locations reachable only through other heap locations. */
    public static Map<Integer, IValue> unsafeGarbageCollector(Set<Integer> symTableAddr,
                                                            Map<Integer, IValue> heap) {
        return heap.entrySet().stream()
                .filter(entry -> symTableAddr.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
