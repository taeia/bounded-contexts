package library.catalogue.application.searchcatalogue;

import java.util.List;

/**
 * The response model: the words that were searched for, said again because the
 * presenter has not seen the request and may want to say "Nothing in the catalogue
 * matches Dickens.", and the entries that match, one per book.
 */
public record SearchCatalogueResponse(String words, List<CatalogueEntry> entries) {
}
