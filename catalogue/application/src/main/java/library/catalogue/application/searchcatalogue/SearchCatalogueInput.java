package library.catalogue.application.searchcatalogue;

/**
 * The input boundary of the use case. Unlike the listings of the lending context, a
 * search has a request model: the words.
 */
public interface SearchCatalogueInput {

    void execute(SearchCatalogueRequest request, SearchCatalogueOutput output);
}
