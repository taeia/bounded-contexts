package library.lending.domain;

import java.util.List;

import library.lending.model.Book;
import library.lending.model.Copy;

/**
 * The copies the library owns. A driven port.
 */
public interface CopyRepository {

    List<Copy> all();

    List<Copy> ofBook(Book book);

    /**
     * The copy with that number, or null if there is none.
     */
    Copy numbered(String number);

    void save(Copy copy);
}
