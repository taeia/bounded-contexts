package library.catalogue.application.addbook;

import library.catalogue.application.Catalogue;
import library.catalogue.application.ChangeSignal;
import library.catalogue.application.IsbnLookup;
import library.catalogue.application.Refusal;
import library.catalogue.model.Book;
import library.catalogue.model.CatalogueException;
import library.catalogue.model.Isbn;

/**
 * Add a book to the catalogue by its ISBN. A use case, the one thing the catalogue
 * does that changes it. The cataloguer types a number and nothing else; the use case
 * makes an {@link Isbn} of it, which is where a number that is not an ISBN is refused,
 * checks that the catalogue does not have the book already, and asks the
 * {@link IsbnLookup} what book that is. What comes back is a {@link Book} of this
 * context, whoever answered and in whatever words: the port promised that, and the
 * use case does not know, or ask, that a foreign service is behind it. Then it saves
 * the book, rings the signal, and presents what it catalogued.
 */
public class AddBook implements AddBookInput {

    private final Catalogue catalogue;
    private final IsbnLookup lookup;
    private final ChangeSignal signal;

    public AddBook(Catalogue catalogue, IsbnLookup lookup, ChangeSignal signal) {
        this.catalogue = catalogue;
        this.lookup = lookup;
        this.signal = signal;
    }

    @Override
    public void execute(AddBookRequest request, AddBookOutput output) {
        Isbn isbn;
        try {
            isbn = Isbn.of(request.isbn());
        } catch (CatalogueException e) {
            throw new Refusal(e.getMessage());
        }
        if (catalogue.withIsbn(isbn) != null) {
            throw new Refusal(isbn + " is already in the catalogue.");
        }
        Book book = lookup.find(isbn);
        if (book == null) {
            throw new Refusal("Nothing is known about " + isbn + ".");
        }
        catalogue.save(book);
        signal.changed();
        output.present(new AddBookResponse(book.isbn().toString(), book.title(), book.authors()));
    }
}
