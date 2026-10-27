package library.catalogue.inmemory;

import java.util.List;

import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The starting catalogue: the three books of Chapter 0, with their authors, which
 * Chapter 0 did not need to say. It is the catalogue context's half of the starting
 * state; the lending context's half, the members and the copies, is loaded by its
 * own class in its own module, and a configurator calls both. The ISBNs are the ones
 * in Chapter 0's table, and the two halves agree on them without either knowing
 * that the other exists.
 */
public class StartingCatalogue {

    public static void load(InMemoryCatalogue catalogue) {
        catalogue.clear();
        catalogue.save(new Book(Isbn.of("978-0-547-92822-7"), "The Hobbit", List.of("J. R. R. Tolkien")));
        catalogue.save(new Book(Isbn.of("978-0-14-143951-8"), "Pride and Prejudice", List.of("Jane Austen")));
        catalogue.save(new Book(Isbn.of("978-0-451-52493-5"), "Nineteen Eighty-Four", List.of("George Orwell")));
    }
}
