package library.catalogue.application.searchcatalogue;

import java.util.List;

/**
 * One book as a search shows it: the ISBN as it was written, the title, and the
 * authors. A record with no behaviour, the catalogue's counterpart of the lending
 * context's BookSummary, under a name of its own so that a presenter that sees both
 * can tell them apart.
 */
public record CatalogueEntry(String isbn, String title, List<String> authors) {
}
