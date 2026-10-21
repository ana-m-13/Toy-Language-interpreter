package interpreter.model.adt;

import interpreter.exceptions.ADTException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ADTTest {
    @Test
    void stackLifoAndEmptyPopThrows() throws ADTException {
        IStack<Integer> stack = new MyStack<>();
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertTrue(stack.isEmpty());
        assertThrows(ADTException.class, stack::pop);
    }

    @Test
    void dictionaryPutLookupUpdateAndMissingKeyThrows() throws ADTException {
        IDictionary<String, Integer> dict = new MyDictionary<>();
        dict.put("a", 10);
        assertTrue(dict.isDefined("a"));
        assertEquals(10, dict.lookup("a"));
        dict.update("a", 20);
        assertEquals(20, dict.lookup("a"));
        assertThrows(ADTException.class, () -> dict.lookup("missing"));
        assertThrows(ADTException.class, () -> dict.update("missing", 5));
    }

    @Test
    void listAddGetSizeAndOutOfBoundsThrows() throws ADTException {
        IList<String> list = new MyList<>();
        list.add("first");
        assertEquals(1, list.size());
        assertEquals("first", list.get(0));
        assertThrows(ADTException.class, () -> list.get(1));
        assertThrows(ADTException.class, () -> list.get(-1));
    }
}