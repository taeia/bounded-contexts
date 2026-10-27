package library.catalogue.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BookTest {

    private final Book goodOmens = new Book(Isbn.of("978-0-06-085398-3"), "Good Omens",
            List.of("Terry Pratchett", "Neil Gaiman"));

    @Test
    void matchesWordsOfTheTitle() {
        assertTrue(goodOmens.matches("Omens"));
        assertTrue(goodOmens.matches("good om"));
        assertFalse(goodOmens.matches("Hobbit"));
    }

    @Test
    void matchesWordsOfAnyAuthor() {
        assertTrue(goodOmens.matches("Pratchett"));
        assertTrue(goodOmens.matches("Gaiman"));
        assertFalse(goodOmens.matches("Tolkien"));
    }

    @Test
    void matchesInAnyCase() {
        assertTrue(goodOmens.matches("GOOD OMENS"));
        assertTrue(goodOmens.matches("neil"));
    }

    @Test
    void noWordsMatchEveryBook() {
        assertTrue(goodOmens.matches(""));
        assertTrue(goodOmens.matches("   "));
        assertTrue(goodOmens.matches(null));
    }

    @Test
    void aBookNeedsATitle() {
        CatalogueException e = assertThrows(CatalogueException.class,
                () -> new Book(Isbn.of("978-0-06-085398-3"), " ", List.of()));
        assertEquals("A book needs a title.", e.getMessage());
    }

    @Test
    void theAuthorsAreACopy() {
        assertEquals(List.of("Terry Pratchett", "Neil Gaiman"), goodOmens.authors());
        assertThrows(UnsupportedOperationException.class, () -> goodOmens.authors().add("Someone"));
    }
}
