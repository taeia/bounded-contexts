package library.lending.application.listbooks;

import java.util.List;

/**
 * The response model: the books, one summary each.
 */
public record ListBooksResponse(List<BookSummary> books) {
}
