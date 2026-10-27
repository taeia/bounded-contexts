package library.catalogue.application.addbook;

import java.util.List;

/**
 * The response model: what the catalogue has to say once a book is in it. The ISBN
 * as it was written, the title, and the authors as a list, because how two authors
 * are joined, with "and" in a sentence or a comma in a table, is the presenter's
 * decision.
 */
public record AddBookResponse(String isbn, String title, List<String> authors) {
}
