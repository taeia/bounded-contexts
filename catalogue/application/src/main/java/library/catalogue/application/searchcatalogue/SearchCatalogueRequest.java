package library.catalogue.application.searchcatalogue;

/**
 * The request model: the words to look for, in a title or an author's name. Empty
 * means everything.
 */
public record SearchCatalogueRequest(String words) {
}
