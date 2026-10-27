package library.lending.inmemory;

import java.util.ArrayList;
import java.util.List;

import library.lending.model.Book;
import library.lending.domain.BookRepository;

public class InMemoryBookRepository implements BookRepository {

    private final List<Book> books = new ArrayList<>();

    @Override
    public List<Book> all() {
        return new ArrayList<>(books);
    }

    @Override
    public Book titled(String title) {
        for (Book book : books) {
            if (book.title().equals(title)) {
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
