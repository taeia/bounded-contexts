package library.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.babel.books.BabelClient;
import library.catalogue.application.addbook.AddBook;
import library.catalogue.application.searchcatalogue.SearchCatalogue;
import library.catalogue.babel.BabelLookup;
import library.catalogue.inmemory.InMemoryCatalogue;
import library.catalogue.inmemory.StartingCatalogue;
import library.lending.application.borrowcopy.BorrowCopy;
import library.lending.application.listcopies.ListCopies;
import library.lending.application.listmembers.ListMembers;
import library.lending.application.listreservations.ListReservations;
import library.lending.application.payfine.PayFine;
import library.lending.application.renewloan.RenewLoan;
import library.lending.application.reservebook.ReserveBook;
import library.lending.application.returncopy.ReturnCopy;
import library.lending.domain.Lending;
import library.lending.domain.Reserving;
import library.lending.inmemory.InMemoryBookRepository;
import library.lending.inmemory.InMemoryCopyRepository;
import library.lending.inmemory.InMemoryMemberRepository;
import library.lending.inmemory.InMemoryNotifier;
import library.lending.inmemory.InMemoryQueueRepository;
import library.lending.inmemory.SettableClock;
import library.lending.inmemory.StartingState;

/**
 * Tests of the HTTP adapter: what it answers for what it is sent. The library behind it
 * is the real use cases of both contexts with the in-memory adapters and the
 * Anti-Corruption Layer on Babel, which this module sees in test scope only, and the
 * server listens on whatever port is free. The test also listens on both change
 * signals with one counter, counting, which is all a listener can do with them.
 */
class HttpApiTest {

    private final AtomicInteger signals = new AtomicInteger();
    private final SettableClock clock = new SettableClock();
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpApi api;

    @BeforeEach
    void start() throws IOException {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryCopyRepository copies = new InMemoryCopyRepository();
        InMemoryMemberRepository members = new InMemoryMemberRepository();
        InMemoryQueueRepository queues = new InMemoryQueueRepository();
        StartingState.load(books, copies, members, queues);
        Lending lending = new Lending(members, copies, queues, clock);
        Reserving reserving = new Reserving(copies, queues);
        InMemoryCatalogue catalogue = new InMemoryCatalogue();
        StartingCatalogue.load(catalogue);
        api = new HttpApi(new BorrowCopy(lending, members, copies, signals::incrementAndGet),
                new ReturnCopy(lending, members, copies, new InMemoryNotifier(), signals::incrementAndGet),
                new RenewLoan(lending, members, copies, signals::incrementAndGet),
                new ReserveBook(reserving, books, members, queues, signals::incrementAndGet),
                new PayFine(members, signals::incrementAndGet),
                new ListCopies(copies, members),
                new ListMembers(members),
                new ListReservations(queues),
                new AddBook(catalogue, new BabelLookup(new BabelClient()), signals::incrementAndGet),
                new SearchCatalogue(catalogue));
        api.start(0, null);
        clock.set(LocalDate.of(2026, 3, 2));
    }

    @AfterEach
    void stop() {
        api.stop();
    }

    @Test
    void aCommandAnswersWithTheSentence() throws Exception {
        HttpResponse<String> response = post("/borrow?member=John&copy=C1");
        assertEquals(200, response.statusCode());
        assertEquals("John borrowed C1 of The Hobbit. It is due on 16 March.", response.body());
    }

    @Test
    void aRefusalIsAConflict() throws Exception {
        post("/borrow?member=John&copy=C1");
        HttpResponse<String> response = post("/borrow?member=Jane&copy=C1");
        assertEquals(409, response.statusCode());
        assertEquals("The library refuses. C1 is on loan.", response.body());
    }

    @Test
    void aListingShowsTheLibrary() throws Exception {
        post("/borrow?member=John&copy=C1");
        post("/reserve?member=Mary&book=The%20Hobbit");
        HttpResponse<String> response = get("/copies");
        assertEquals(200, response.statusCode());
        assertEquals("""
                C1  The Hobbit: on loan to John, due 16 March
                C2  Pride and Prejudice: available
                C3  Pride and Prejudice: available
                C4  Nineteen Eighty-Four: available
                C5  Nineteen Eighty-Four: available
                """, response.body());
        assertEquals("The Hobbit: Mary, position 1\n", get("/reservations").body());
    }

    @Test
    void aMistakeIsAnswered() throws Exception {
        assertEquals(400, post("/borrow?member=John").statusCode());
        assertEquals("The request needs a copy.", post("/borrow?member=John").body());
        assertEquals(404, get("/borrow?member=John&copy=C1").statusCode());
        assertEquals(404, post("/lend?member=John&copy=C1").statusCode());
    }

    @Test
    void theSignalRingsOncePerCompletedCommand() throws Exception {
        post("/borrow?member=John&copy=C1");
        post("/borrow?member=Jane&copy=C1");
        get("/copies");
        post("/reserve?member=Mary&book=The%20Hobbit");
        assertEquals(2, signals.get());
    }

    @Test
    void cataloguingAnswersWithTheSentenceAndRingsTheSignal() throws Exception {
        HttpResponse<String> response = post("/catalogue?isbn=978-0-452-28424-1");
        assertEquals(200, response.statusCode());
        assertEquals("Animal Farm by George Orwell is now in the catalogue.", response.body());
        assertEquals(1, signals.get());
    }

    @Test
    void theCatalogueIsSearchedAndListed() throws Exception {
        post("/catalogue?isbn=978-0-06-085398-3");
        assertEquals("978-0-06-085398-3  Good Omens by Terry Pratchett and Neil Gaiman\n",
                get("/catalogue?q=Gaiman").body());
        assertEquals("""
                978-0-547-92822-7  The Hobbit by J. R. R. Tolkien
                978-0-14-143951-8  Pride and Prejudice by Jane Austen
                978-0-451-52493-5  Nineteen Eighty-Four by George Orwell
                978-0-06-085398-3  Good Omens by Terry Pratchett and Neil Gaiman
                """, get("/catalogue").body());
    }

    @Test
    void aCatalogueRefusalIsAConflictToo() throws Exception {
        HttpResponse<String> response = post("/catalogue?isbn=978-0-547-92822-7");
        assertEquals(409, response.statusCode());
        assertEquals("The catalogue refuses. 978-0-547-92822-7 is already in the catalogue.", response.body());
        assertEquals(400, post("/catalogue").statusCode());
        assertEquals(0, signals.get());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + api.port() + path);
    }
}
