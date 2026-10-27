package library.catalogue.model;

import java.util.List;

/**
 * A book as the catalogue knows it: a title, its authors, and the ISBN that identifies
 * it. An aggregate root with nothing inside it but itself. This is the catalogue's
 * model of a book, and it is not the lending context's: that one has an ISBN and a
 * title and is the thing copies are copies of and queues form for; this one has
 * authors and is the thing a reader searches for. The two classes share a name and
 * thirteen digits, and nothing else, and neither can see the other. The one rule the
 * catalogue has about a book is here: what it means for a book to match a search.
 */
public class Book {

    private final Isbn isbn;
    private final String title;
    private final List<String> authors;

    public Book(Isbn isbn, String title, List<String> authors) {
        if (title == null || title.isBlank()) {
            throw new CatalogueException("A book needs a title.");
        }
        this.isbn = isbn;
        this.title = title;
        this.authors = List.copyOf(authors);
    }

    public Isbn isbn() {
        return isbn;
    }

    public String title() {
        return title;
    }

    /**
     * The authors in the order the catalogue lists them. The list is a copy, and may
     * be empty: a catalogue has books nobody signed.
     */
    public List<String> authors() {
        return authors;
    }

    /**
     * Whether the book answers a search: the words appear in the title or in an
     * author's name, in any case. No words match every book, which is how a listing
     * is a search for nothing in particular.
     */
    public boolean matches(String words) {
        if (words == null || words.isBlank()) {
            return true;
        }
        String wanted = words.strip().toLowerCase();
        if (title.toLowerCase().contains(wanted)) {
            return true;
        }
        for (String author : authors) {
            if (author.toLowerCase().contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return title;
    }
}
