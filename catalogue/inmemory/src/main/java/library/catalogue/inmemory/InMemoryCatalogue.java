package library.catalogue.inmemory;

import java.util.ArrayList;
import java.util.List;

import library.catalogue.application.Catalogue;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The catalogue kept in a list. A driven adapter of the catalogue context, as the
 * lending context's in-memory repositories are of theirs.
 */
public class InMemoryCatalogue implements Catalogue {

    private final List<Book> books = new ArrayList<>();

    @Override
    public List<Book> all() {
        return new ArrayList<>(books);
    }

    @Override
    public Book withIsbn(Isbn isbn) {
        for (Book book : books) {
            if (book.isbn().equals(isbn)) {
                return book;
            }
        }
        return null;
    }

    @Override
    public void save(Book book) {
        if (!books.contains(book)) {
            books.add(book);
        }
    }

    public void clear() {
        books.clear();
    }
}
