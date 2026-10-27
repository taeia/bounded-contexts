package library.catalogue.application.searchcatalogue;

import java.util.ArrayList;
import java.util.List;

import library.catalogue.application.Catalogue;
import library.catalogue.model.Book;

/**
 * Search the catalogue. A use case of the asking kind: it changes nothing and rings no
 * signal. It asks every book whether it matches the words and answers with the ones
 * that do, one {@link CatalogueEntry} each, in the order they were catalogued. With no
 * words every book matches, so this is also the listing: the window's catalogue table
 * is a search for nothing in particular.
 */
public class SearchCatalogue implements SearchCatalogueInput {

    private final Catalogue catalogue;

    public SearchCatalogue(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void execute(SearchCatalogueRequest request, SearchCatalogueOutput output) {
        List<CatalogueEntry> result = new ArrayList<>();
        for (Book book : catalogue.all()) {
            if (book.matches(request.words())) {
                result.add(new CatalogueEntry(book.isbn().toString(), book.title(), book.authors()));
            }
        }
        output.present(new SearchCatalogueResponse(request.words(), result));
    }
}
