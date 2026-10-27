package library.catalogue.application;

import java.util.List;

import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The books the library knows. A driven port, the repository of this context's one
 * aggregate, declared here because the use cases are its only callers: the catalogue
 * has no ring of domain services to declare it in. It is the whole catalogue as a
 * collection, which is what a repository is, and so it carries the context's name.
 */
public interface Catalogue {

    /**
     * Every book, in the order they were catalogued.
     */
    List<Book> all();

    /**
     * The book with that ISBN, or null if the catalogue does not have it.
     */
    Book withIsbn(Isbn isbn);

    void save(Book book);
}
