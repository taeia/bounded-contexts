package library.catalogue.application.searchcatalogue;

/**
 * The output boundary of the use case, implemented by a presenter on the outside.
 */
public interface SearchCatalogueOutput {

    void present(SearchCatalogueResponse response);
}
