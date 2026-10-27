package library.catalogue.babel;

import java.util.ArrayList;
import java.util.List;

import com.babel.books.BabelRecord;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * From Babel's model of a book to the catalogue's. The translator of the
 * Anti-Corruption Layer, and the one class that knows both models well enough to
 * say what corresponds to what. Four decisions are written here and nowhere else:
 * Babel's title is the catalogue's title and Babel's subtitle is nothing; a
 * contributor with a role in brackets is not an author; "Tolkien, J. R. R." is
 * "J. R. R. Tolkien"; and the year and the publisher are not kept, because the
 * catalogue has no field for them and wants none. The ISBN is the one the cataloguer
 * typed, not Babel's bare digits, so that the catalogue shows it as it was written.
 */
final class BabelTranslator {

    private BabelTranslator() {
    }

    static Book toBook(Isbn isbn, BabelRecord record) {
        List<String> authors = new ArrayList<>();
        for (String contributor : record.contributors()) {
            if (!hasRole(contributor)) {
                authors.add(naturalOrder(contributor));
            }
        }
        return new Book(isbn, record.title(), authors);
    }

    /**
     * Babel marks everyone who is not the author with a role in brackets:
     * "Lee, Alan (illustrator)".
     */
    private static boolean hasRole(String contributor) {
        return contributor.endsWith(")");
    }

    /**
     * Babel writes "Orwell, George"; the catalogue writes "George Orwell". A name with
     * no comma is left as it is.
     */
    private static String naturalOrder(String contributor) {
        int comma = contributor.indexOf(',');
        if (comma < 0) {
            return contributor.strip();
        }
        return contributor.substring(comma + 1).strip() + " " + contributor.substring(0, comma).strip();
    }
}
