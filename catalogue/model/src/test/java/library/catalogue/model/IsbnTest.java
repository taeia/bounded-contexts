package library.catalogue.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IsbnTest {

    @Test
    void theIsbnsOfChapter0AreIsbns() {
        assertEquals("9780547928227", Isbn.of("978-0-547-92822-7").digits());
        assertEquals("9780141439518", Isbn.of("978-0-14-143951-8").digits());
        assertEquals("9780451524935", Isbn.of("978-0-451-52493-5").digits());
    }

    @Test
    void itIsKeptAsWritten() {
        assertEquals("978-0-547-92822-7", Isbn.of("978-0-547-92822-7").toString());
        assertEquals("9780547928227", Isbn.of("9780547928227").toString());
    }

    @Test
    void twoSpellingsOfOneNumberAreOneIsbn() {
        assertEquals(Isbn.of("978-0-547-92822-7"), Isbn.of("9780547928227"));
        assertEquals(Isbn.of("978-0-547-92822-7").hashCode(), Isbn.of("978 0 547 92822 7").hashCode());
        assertNotEquals(Isbn.of("978-0-547-92822-7"), Isbn.of("978-0-14-143951-8"));
    }

    @Test
    void aWrongCheckDigitIsNotAnIsbn() {
        CatalogueException e = assertThrows(CatalogueException.class, () -> Isbn.of("978-0-547-92822-8"));
        assertEquals("978-0-547-92822-8 is not an ISBN.", e.getMessage());
    }

    @Test
    void tooFewOrTooManyDigitsAreNotAnIsbn() {
        assertThrows(CatalogueException.class, () -> Isbn.of("978-0-547-9282"));
        assertThrows(CatalogueException.class, () -> Isbn.of("978-0-547-92822-7-0"));
        assertThrows(CatalogueException.class, () -> Isbn.of("0-547-92822-7"));
    }

    @Test
    void wordsAreNotAnIsbn() {
        CatalogueException e = assertThrows(CatalogueException.class, () -> Isbn.of("hobbit"));
        assertEquals("hobbit is not an ISBN.", e.getMessage());
        assertThrows(CatalogueException.class, () -> Isbn.of(""));
        assertThrows(CatalogueException.class, () -> Isbn.of(null));
    }
}
