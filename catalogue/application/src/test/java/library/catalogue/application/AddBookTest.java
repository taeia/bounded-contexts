package library.catalogue.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import library.catalogue.application.addbook.AddBook;
import library.catalogue.application.addbook.AddBookRequest;
import library.catalogue.application.addbook.AddBookResponse;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The use case on the two fakes: what it saves, what it presents, what it refuses,
 * and when it asks the lookup.
 */
class AddBookTest {

    private final FakeCatalogue catalogue = new FakeCatalogue();
    private final FakeIsbnLookup lookup = new FakeIsbnLookup();
    private final int[] signals = {0};
    private final AddBook addBook = new AddBook(catalogue, lookup, () -> signals[0]++);
    private AddBookResponse presented;

    private final Book hobbit = new Book(Isbn.of("978-0-547-92822-7"), "The Hobbit", List.of("J. R. R. Tolkien"));
    private final Book animalFarm = new Book(Isbn.of("978-0-452-28424-1"), "Animal Farm", List.of("George Orwell"));

    @Test
    void aKnownIsbnIsCataloguedAndPresented() {
        lookup.knows(animalFarm);

        addBook.execute(new AddBookRequest("978-0-452-28424-1"), response -> presented = response);

        assertEquals(List.of(animalFarm), catalogue.books);
        assertEquals("978-0-452-28424-1", presented.isbn());
        assertEquals("Animal Farm", presented.title());
        assertEquals(List.of("George Orwell"), presented.authors());
        assertEquals(1, signals[0]);
    }

    @Test
    void aBookAlreadyInTheCatalogueIsRefusedWithoutAsking() {
        catalogue.save(hobbit);
        lookup.knows(hobbit);

        Refusal refusal = assertThrows(Refusal.class,
                () -> addBook.execute(new AddBookRequest("9780547928227"), response -> presented = response));

        assertEquals("9780547928227 is already in the catalogue.", refusal.getMessage());
        assertEquals(0, lookup.questions);
        assertNull(presented);
        assertEquals(0, signals[0]);
    }

    @Test
    void anIsbnNobodyKnowsIsRefused() {
        Refusal refusal = assertThrows(Refusal.class,
                () -> addBook.execute(new AddBookRequest("978-1-234-56789-7"), response -> presented = response));

        assertEquals("Nothing is known about 978-1-234-56789-7.", refusal.getMessage());
        assertEquals(1, lookup.questions);
        assertTrue(catalogue.books.isEmpty());
    }

    @Test
    void somethingThatIsNotAnIsbnIsRefusedBeforeAnyoneIsAsked() {
        Refusal refusal = assertThrows(Refusal.class,
                () -> addBook.execute(new AddBookRequest("hobbit"), response -> presented = response));

        assertEquals("hobbit is not an ISBN.", refusal.getMessage());
        assertEquals(0, lookup.questions);
    }
}
