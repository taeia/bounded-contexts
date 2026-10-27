package library.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.babel.books.BabelClient;
import library.catalogue.application.Refusal;
import library.catalogue.application.addbook.AddBook;
import library.catalogue.application.addbook.AddBookInput;
import library.catalogue.application.addbook.AddBookRequest;
import library.catalogue.application.addbook.AddBookResponse;
import library.catalogue.application.searchcatalogue.CatalogueEntry;
import library.catalogue.application.searchcatalogue.SearchCatalogue;
import library.catalogue.application.searchcatalogue.SearchCatalogueInput;
import library.catalogue.application.searchcatalogue.SearchCatalogueRequest;
import library.catalogue.babel.BabelLookup;
import library.catalogue.inmemory.InMemoryCatalogue;
import library.catalogue.inmemory.StartingCatalogue;
import library.lending.application.listcopies.CopySummary;
import library.lending.application.listcopies.ListCopies;
import library.lending.application.listcopies.ListCopiesInput;
import library.lending.application.reservebook.ReserveBook;
import library.lending.application.reservebook.ReserveBookInput;
import library.lending.application.reservebook.ReserveBookRequest;
import library.lending.domain.Reserving;
import library.lending.inmemory.InMemoryBookRepository;
import library.lending.inmemory.InMemoryCopyRepository;
import library.lending.inmemory.InMemoryMemberRepository;
import library.lending.inmemory.InMemoryQueueRepository;
import library.lending.inmemory.StartingState;

/**
 * One test per catalogue scenario of Chapter 11, with the same name. A driving adapter
 * like {@link ScenarioTest}, with a configurator that builds the catalogue context:
 * its in-memory catalogue, and behind its lookup port the Anti-Corruption Layer on a
 * Babel client, which is the real one, because Babel here is a table. The last
 * scenario crosses to the lending context, so the configurator builds the two lending
 * use cases it needs as well, on lending's own storage; the two contexts are wired
 * side by side and share nothing but this class. The tests name a book the way a
 * cataloguer does, by its ISBN, and read the catalogue back through the search use
 * case, so no test reaches a Book of either context.
 */
class CatalogueScenarioTest {

    private final InMemoryCatalogue catalogue = new InMemoryCatalogue();
    private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
    private final InMemoryCopyRepository copyRepository = new InMemoryCopyRepository();
    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
    private final InMemoryQueueRepository queueRepository = new InMemoryQueueRepository();
    private final Recorder recorder = new Recorder();
    private AddBookInput addBook;
    private SearchCatalogueInput searchCatalogue;
    private ReserveBookInput reserveBook;
    private ListCopiesInput listCopies;

    @BeforeEach
    void startingState() {
        StartingCatalogue.load(catalogue);
        StartingState.load(bookRepository, copyRepository, memberRepository, queueRepository);
        addBook = new AddBook(catalogue, new BabelLookup(new BabelClient()), () -> {});
        searchCatalogue = new SearchCatalogue(catalogue);
        reserveBook = new ReserveBook(new Reserving(copyRepository, queueRepository), bookRepository,
                memberRepository, queueRepository, () -> {});
        listCopies = new ListCopies(copyRepository, memberRepository);
    }

    @Test
    @DisplayName("The catalogue at the start")
    void theCatalogueAtTheStart() {
        assertEquals(List.of(
                entry("978-0-547-92822-7", "The Hobbit", "J. R. R. Tolkien"),
                entry("978-0-14-143951-8", "Pride and Prejudice", "Jane Austen"),
                entry("978-0-451-52493-5", "Nineteen Eighty-Four", "George Orwell")),
                search(""));
    }

    @Test
    @DisplayName("Searching by author")
    void searchingByAuthor() {
        assertEquals(List.of(entry("978-0-14-143951-8", "Pride and Prejudice", "Jane Austen")), search("Austen"));
        assertEquals(List.of(entry("978-0-547-92822-7", "The Hobbit", "J. R. R. Tolkien")), search("Tolkien"));
    }

    @Test
    @DisplayName("Searching by title")
    void searchingByTitle() {
        assertEquals(List.of(entry("978-0-451-52493-5", "Nineteen Eighty-Four", "George Orwell")), search("eighty"));
        assertEquals(List.of(entry("978-0-547-92822-7", "The Hobbit", "J. R. R. Tolkien")), search("HOBBIT"));
    }

    @Test
    @DisplayName("A search that finds nothing")
    void aSearchThatFindsNothing() {
        assertEquals(List.of(), search("Dickens"));
    }

    @Test
    @DisplayName("Cataloguing a book by its ISBN")
    void cataloguingABookByItsIsbn() {
        AddBookResponse added = add("978-0-452-28424-1");
        assertEquals("978-0-452-28424-1", added.isbn());
        assertEquals("Animal Farm", added.title());
        assertEquals(List.of("George Orwell"), added.authors());

        assertEquals(List.of(
                entry("978-0-451-52493-5", "Nineteen Eighty-Four", "George Orwell"),
                entry("978-0-452-28424-1", "Animal Farm", "George Orwell")),
                search("Orwell"));
    }

    @Test
    @DisplayName("A book with two authors")
    void aBookWithTwoAuthors() {
        AddBookResponse added = add("978-0-06-085398-3");
        assertEquals("Good Omens", added.title());
        assertEquals(List.of("Terry Pratchett", "Neil Gaiman"), added.authors());

        assertEquals(List.of(new CatalogueEntry("978-0-06-085398-3", "Good Omens",
                List.of("Terry Pratchett", "Neil Gaiman"))), search("Gaiman"));
    }

    @Test
    @DisplayName("The catalogue takes what it needs")
    void theCatalogueTakesWhatItNeeds() {
        AddBookResponse added = add("978-0-618-64015-7");
        assertEquals("The Lord of the Rings", added.title());
        assertEquals(List.of("J. R. R. Tolkien"), added.authors());
        assertEquals(List.of(), search("Alan Lee"));
    }

    @Test
    @DisplayName("A book cannot be catalogued twice")
    void aBookCannotBeCataloguedTwice() {
        refused(() -> add("978-0-547-92822-7"), "978-0-547-92822-7 is already in the catalogue.");
        refused(() -> add("9780547928227"), "9780547928227 is already in the catalogue.");
        assertEquals(3, search("").size());
    }

    @Test
    @DisplayName("An ISBN nobody knows")
    void anIsbnNobodyKnows() {
        refused(() -> add("978-1-234-56789-7"), "Nothing is known about 978-1-234-56789-7.");
        assertEquals(3, search("").size());
    }

    @Test
    @DisplayName("Something that is not an ISBN")
    void somethingThatIsNotAnIsbn() {
        refused(() -> add("978-0-547-92822-8"), "978-0-547-92822-8 is not an ISBN.");
        refused(() -> add("hobbit"), "hobbit is not an ISBN.");
        assertEquals(3, search("").size());
    }

    @Test
    @DisplayName("Cataloguing adds no copy")
    void cataloguingAddsNoCopy() {
        add("978-0-452-28424-1");

        listCopies.execute(recorder);
        assertEquals(List.of("C1", "C2", "C3", "C4", "C5"),
                recorder.copies.copies().stream().map(CopySummary::number).toList());
        library.lending.application.Refusal refusal = assertThrows(library.lending.application.Refusal.class,
                () -> reserveBook.execute(new ReserveBookRequest("John", "Animal Farm"), recorder));
        assertEquals("There is no book called Animal Farm.", refusal.getMessage());
    }

    // ---- helpers ----

    private AddBookResponse add(String isbn) {
        addBook.execute(new AddBookRequest(isbn), recorder);
        return recorder.added;
    }

    private List<CatalogueEntry> search(String words) {
        searchCatalogue.execute(new SearchCatalogueRequest(words), recorder);
        return recorder.found.entries();
    }

    private static CatalogueEntry entry(String isbn, String title, String author) {
        return new CatalogueEntry(isbn, title, List.of(author));
    }

    private void refused(Executable operation, String reason) {
        Refusal refusal = assertThrows(Refusal.class, operation);
        assertEquals(reason, refusal.getMessage());
    }
}
