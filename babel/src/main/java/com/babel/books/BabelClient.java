package com.babel.books;

import java.util.List;
import java.util.Map;

/**
 * The client of the Babel book service, which knows every book that was ever
 * printed, or says it does. This is the vendor's class: in a real system it would come
 * in the vendor's jar and {@link #fetch} would be an HTTP call. Here it answers from
 * a table of seven books, which is enough for the catalogue scenarios, and it answers
 * in the vendor's manner: thirteen digits in, a {@link BabelRecord} or null out, and
 * an exception for a request the service would not accept. Nothing here knows what
 * the library calls anything.
 */
public final class BabelClient {

    private static final Map<String, BabelRecord> BOOKS = Map.of(
            "9780547928227", new BabelRecord("9780547928227", "The Hobbit", "or There and Back Again",
                    List.of("Tolkien, J. R. R."), 1937, "George Allen & Unwin"),
            "9780141439518", new BabelRecord("9780141439518", "Pride and Prejudice", null,
                    List.of("Austen, Jane"), 1813, "T. Egerton"),
            "9780451524935", new BabelRecord("9780451524935", "Nineteen Eighty-Four", null,
                    List.of("Orwell, George"), 1949, "Secker & Warburg"),
            "9780452284241", new BabelRecord("9780452284241", "Animal Farm", "A Fairy Story",
                    List.of("Orwell, George"), 1945, "Secker & Warburg"),
            "9780141439587", new BabelRecord("9780141439587", "Emma", null,
                    List.of("Austen, Jane"), 1815, "John Murray"),
            "9780060853983", new BabelRecord("9780060853983", "Good Omens",
                    "The Nice and Accurate Prophecies of Agnes Nutter, Witch",
                    List.of("Pratchett, Terry", "Gaiman, Neil"), 1990, "Gollancz"),
            "9780618640157", new BabelRecord("9780618640157", "The Lord of the Rings", null,
                    List.of("Tolkien, J. R. R.", "Lee, Alan (illustrator)"), 1954, "George Allen & Unwin"));

    /**
     * The record for the ISBN, or null when Babel has none. The ISBN must be exactly
     * thirteen digits; anything else is a malformed request.
     */
    public BabelRecord fetch(String isbn13) {
        if (isbn13 == null || !isbn13.matches("\\d{13}")) {
            throw new BabelException("isbn13 must be 13 digits, was: " + isbn13);
        }
        return BOOKS.get(isbn13);
    }
}
