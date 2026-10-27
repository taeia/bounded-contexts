package library.catalogue.application;

import java.util.HashMap;
import java.util.Map;

import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The lookup port, answered from a map, for the tests of this circle. It counts the
 * questions, so that a test can check the catalogue did not ask about a book it
 * already had. No test in this circle knows that Babel exists.
 */
class FakeIsbnLookup implements IsbnLookup {

    private final Map<Isbn, Book> known = new HashMap<>();
    int questions;

    void knows(Book book) {
        known.put(book.isbn(), book);
    }

    @Override
    public Book find(Isbn isbn) {
        questions++;
        return known.get(isbn);
    }
}
