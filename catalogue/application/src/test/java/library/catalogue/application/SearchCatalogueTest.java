package library.catalogue.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import library.catalogue.application.searchcatalogue.CatalogueEntry;
import library.catalogue.application.searchcatalogue.SearchCatalogue;
import library.catalogue.application.searchcatalogue.SearchCatalogueRequest;
import library.catalogue.application.searchcatalogue.SearchCatalogueResponse;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

class SearchCatalogueTest {

    private final FakeCatalogue catalogue = new FakeCatalogue();
    private final SearchCatalogue search = new SearchCatalogue(catalogue);
    private SearchCatalogueResponse presented;

    @BeforeEach
    void twoBooks() {
        catalogue.save(new Book(Isbn.of("978-0-547-92822-7"), "The Hobbit", List.of("J. R. R. Tolkien")));
        catalogue.save(new Book(Isbn.of("978-0-451-52493-5"), "Nineteen Eighty-Four", List.of("George Orwell")));
    }

    @Test
    void noWordsListsTheWholeCatalogueInOrder() {
        search.execute(new SearchCatalogueRequest(""), response -> presented = response);

        assertEquals("", presented.words());
        assertEquals(List.of(
                new CatalogueEntry("978-0-547-92822-7", "The Hobbit", List.of("J. R. R. Tolkien")),
                new CatalogueEntry("978-0-451-52493-5", "Nineteen Eighty-Four", List.of("George Orwell"))),
                presented.entries());
    }

    @Test
    void wordsKeepTheBooksThatMatchAndSayWhatWasAskedFor() {
        search.execute(new SearchCatalogueRequest("Orwell"), response -> presented = response);

        assertEquals("Orwell", presented.words());
        assertEquals(List.of(new CatalogueEntry("978-0-451-52493-5", "Nineteen Eighty-Four", List.of("George Orwell"))),
                presented.entries());
    }

    @Test
    void aSearchThatFindsNothingAnswersWithNoEntries() {
        search.execute(new SearchCatalogueRequest("Dickens"), response -> presented = response);

        assertEquals(List.of(), presented.entries());
    }
}
