package library.catalogue.application;

import java.util.ArrayList;
import java.util.List;

import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The repository port, answered with a list, for the tests of this circle.
 */
class FakeCatalogue implements Catalogue {

    final List<Book> books = new ArrayList<>();

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
}
