package library.catalogue.babel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.babel.books.BabelClient;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The translation, run against Babel's own answers: what the catalogue gets for what
 * Babel says.
 */
class BabelLookupTest {

    private final BabelLookup lookup = new BabelLookup(new BabelClient());

    @Test
    void aSurnameFirstContributorBecomesAnAuthorInNaturalOrder() {
        Book book = lookup.find(Isbn.of("978-0-452-28424-1"));

        assertEquals("Animal Farm", book.title());
        assertEquals(List.of("George Orwell"), book.authors());
    }

    @Test
    void theSubtitleIsNotTheTitle() {
        assertEquals("The Hobbit", lookup.find(Isbn.of("978-0-547-92822-7")).title());
    }

    @Test
    void twoContributorsAreTwoAuthorsInBabelsOrder() {
        assertEquals(List.of("Terry Pratchett", "Neil Gaiman"), lookup.find(Isbn.of("978-0-06-085398-3")).authors());
    }

    @Test
    void aContributorWithARoleIsNotAnAuthor() {
        assertEquals(List.of("J. R. R. Tolkien"), lookup.find(Isbn.of("978-0-618-64015-7")).authors());
    }

    @Test
    void theIsbnIsTheOneThatWasAsked() {
        Book book = lookup.find(Isbn.of("978-0-14-143958-7"));

        assertEquals("978-0-14-143958-7", book.isbn().toString());
        assertEquals("Emma", book.title());
    }

    @Test
    void whatBabelDoesNotKnowIsNull() {
        assertNull(lookup.find(Isbn.of("978-1-234-56789-7")));
    }
}
