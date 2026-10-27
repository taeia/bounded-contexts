package library.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

import com.babel.books.BabelClient;
import library.catalogue.application.addbook.AddBook;
import library.catalogue.application.addbook.AddBookInput;
import library.catalogue.application.addbook.AddBookRequest;
import library.catalogue.application.searchcatalogue.SearchCatalogue;
import library.catalogue.application.searchcatalogue.SearchCatalogueInput;
import library.catalogue.application.searchcatalogue.SearchCatalogueRequest;
import library.catalogue.babel.BabelLookup;
import library.catalogue.inmemory.InMemoryCatalogue;
import library.catalogue.inmemory.StartingCatalogue;
import library.lending.application.Refusal;
import library.lending.application.borrowcopy.BorrowCopy;
import library.lending.application.borrowcopy.BorrowCopyInput;
import library.lending.application.borrowcopy.BorrowCopyRequest;
import library.lending.application.listcopies.ListCopies;
import library.lending.application.listcopies.ListCopiesInput;
import library.lending.application.listmembers.ListMembers;
import library.lending.application.listmembers.ListMembersInput;
import library.lending.application.listreservations.ListReservations;
import library.lending.application.listreservations.ListReservationsInput;
import library.lending.application.payfine.PayFine;
import library.lending.application.payfine.PayFineInput;
import library.lending.application.payfine.PayFineRequest;
import library.lending.application.renewloan.RenewLoan;
import library.lending.application.renewloan.RenewLoanInput;
import library.lending.application.renewloan.RenewLoanRequest;
import library.lending.application.reservebook.ReserveBook;
import library.lending.application.reservebook.ReserveBookInput;
import library.lending.application.reservebook.ReserveBookRequest;
import library.lending.application.returncopy.ReturnCopy;
import library.lending.application.returncopy.ReturnCopyInput;
import library.lending.application.returncopy.ReturnCopyRequest;
import library.lending.domain.Lending;
import library.lending.domain.Reserving;
import library.lending.inmemory.InMemoryBookRepository;
import library.lending.inmemory.InMemoryCopyRepository;
import library.lending.inmemory.InMemoryMemberRepository;
import library.lending.inmemory.InMemoryQueueRepository;
import library.lending.inmemory.SettableClock;
import library.lending.inmemory.StartingState;
import library.presenters.TextPresenter;

/**
 * The library on the command line. A driving adapter, and in Martin's words a
 * controller and a view in one class: it reads commands written in the words of
 * Chapter 0 and of Chapter 11, turns each line into a request model and calls the use
 * case behind the input boundary, and prints what the presenter made of the response.
 * It holds use cases of both contexts, and it is one of the few classes that do: a
 * driver is outside both, and the two contexts meet here in the way they meet in the
 * window, side by side and never through each other. Each context has a Refusal of
 * its own, so this class catches two, and says which one refused. It names a use
 * case class only in {@link #main}, which is the configurator for this program.
 *
 * Commands are one per line. {@code day 2 March} moves the clock. {@code borrow John C1},
 * {@code return C1}, {@code renew C1}, {@code reserve Mary The Hobbit}, and
 * {@code pay Jane} are the five lending operations. {@code copies}, {@code members}, and
 * {@code reservations} show the library. {@code add 978-0-452-28424-1} catalogues a
 * book by its ISBN, {@code search Orwell} searches the catalogue, and
 * {@code catalogue} lists it. {@code reset} puts both contexts back in their starting
 * state, and {@code quit} ends the session. Dates are in 2026, the year of the
 * scenarios.
 */
public class Cli {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_WITH_YEAR = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM uuuu")
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    private final BorrowCopyInput borrowCopy;
    private final ReturnCopyInput returnCopy;
    private final RenewLoanInput renewLoan;
    private final ReserveBookInput reserveBook;
    private final PayFineInput payFine;
    private final ListCopiesInput listCopies;
    private final ListMembersInput listMembers;
    private final ListReservationsInput listReservations;
    private final AddBookInput addBook;
    private final SearchCatalogueInput searchCatalogue;
    private final TextPresenter presenter = new TextPresenter();
    private final SettableClock clock;
    private final Runnable reset;
    private final PrintStream out;

    /**
     * Ten use cases, one by one, eight from one context and two from the other, where
     * Chapter 10 took eight. The count is still the point.
     */
    public Cli(BorrowCopyInput borrowCopy, ReturnCopyInput returnCopy, RenewLoanInput renewLoan,
               ReserveBookInput reserveBook, PayFineInput payFine, ListCopiesInput listCopies,
               ListMembersInput listMembers, ListReservationsInput listReservations,
               AddBookInput addBook, SearchCatalogueInput searchCatalogue,
               SettableClock clock, Runnable reset, PrintStream out) {
        this.borrowCopy = borrowCopy;
        this.returnCopy = returnCopy;
        this.renewLoan = renewLoan;
        this.reserveBook = reserveBook;
        this.payFine = payFine;
        this.listCopies = listCopies;
        this.listMembers = listMembers;
        this.listReservations = listReservations;
        this.addBook = addBook;
        this.searchCatalogue = searchCatalogue;
        this.clock = clock;
        this.reset = reset;
        this.out = out;
    }

    /**
     * Reads commands from a script file if one is given, otherwise from the terminal.
     */
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ENGLISH);

        // The configurator for the command line: both contexts, each with its own
        // storage, wired one after the other and never to each other. The lending
        // context as in Chapter 10, with a mailbox that prints and nobody on the
        // change signal. The catalogue context with its in-memory catalogue and, behind
        // its lookup port, the Anti-Corruption Layer on a Babel client; this is the one
        // line in this program that names Babel. Reset reloads both starting states.
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryCopyRepository copies = new InMemoryCopyRepository();
        InMemoryMemberRepository members = new InMemoryMemberRepository();
        InMemoryQueueRepository queues = new InMemoryQueueRepository();
        SettableClock clock = new SettableClock();
        ConsoleNotifier notifier = new ConsoleNotifier(System.out);
        Lending lending = new Lending(members, copies, queues, clock);
        Reserving reserving = new Reserving(copies, queues);
        InMemoryCatalogue catalogue = new InMemoryCatalogue();
        BabelLookup lookup = new BabelLookup(new BabelClient());
        Runnable reset = () -> {
            StartingState.load(books, copies, members, queues);
            StartingCatalogue.load(catalogue);
        };
        reset.run();
        Cli cli = new Cli(new BorrowCopy(lending, members, copies, () -> {}),
                new ReturnCopy(lending, members, copies, notifier, () -> {}),
                new RenewLoan(lending, members, copies, () -> {}),
                new ReserveBook(reserving, books, members, queues, () -> {}),
                new PayFine(members, () -> {}),
                new ListCopies(copies, members),
                new ListMembers(members),
                new ListReservations(queues),
                new AddBook(catalogue, lookup, () -> {}),
                new SearchCatalogue(catalogue),
                clock, reset, System.out);

        if (args.length > 0) {
            try (BufferedReader script = Files.newBufferedReader(Path.of(args[0]))) {
                cli.run(script, true);
            }
        } else {
            cli.run(new BufferedReader(new InputStreamReader(System.in)), false);
        }
    }

    /**
     * Runs commands until the input ends or a {@code quit}. When reading a script, each
     * command is echoed behind a prompt, so that the output reads like a session.
     */
    public void run(BufferedReader in, boolean echo) throws IOException {
        if (!echo) {
            out.print("> ");
        }
        String line;
        while ((line = in.readLine()) != null) {
            line = line.strip();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (echo) {
                    out.println("> " + line);
                }
                out.println(execute(line));
                if (line.equals("quit")) {
                    return;
                }
            }
            if (!echo) {
                out.print("> ");
            }
        }
    }

    /**
     * Executes one command and returns the sentence to print. Two contexts, two
     * refusals: the lending one is imported, and the catalogue's is written out in
     * full, because a class cannot import two names that are the same.
     */
    public String execute(String line) {
        String[] words = line.strip().split("\\s+", 2);
        String command = words[0];
        String rest = words.length > 1 ? words[1].strip().replaceAll("\\s+", " ") : "";
        try {
            return switch (command) {
                case "day" -> day(rest);
                case "borrow" -> borrow(rest);
                case "return" -> returnCopy(rest);
                case "renew" -> renew(rest);
                case "reserve" -> reserve(rest);
                case "pay" -> pay(rest);
                case "copies" -> copies();
                case "members" -> members();
                case "reservations" -> reservations();
                case "add" -> add(rest);
                case "search" -> search(rest);
                case "catalogue" -> search("");
                case "reset" -> reset();
                case "help" -> help();
                case "quit" -> "Goodbye.";
                default -> "Unknown command: " + command + ". Type help.";
            };
        } catch (Refusal e) {
            return "The library refuses. " + e.getMessage();
        } catch (library.catalogue.application.Refusal e) {
            return "The catalogue refuses. " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // ---- the clock ----

    private String day(String text) {
        try {
            clock.set(LocalDate.parse(text + " 2026", DAY_WITH_YEAR));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Write the day as in \"day 2 March\".");
        }
        return "It is " + clock.today().format(DAY) + ".";
    }

    // ---- the five lending operations: a request in, the presenter's sentence out ----

    private String borrow(String rest) {
        String[] words = twoWords(rest, "borrow John C1");
        borrowCopy.execute(new BorrowCopyRequest(words[0], words[1]), presenter);
        return presenter.text();
    }

    private String returnCopy(String rest) {
        returnCopy.execute(new ReturnCopyRequest(oneWord(rest, "return C1")), presenter);
        return presenter.text();
    }

    private String renew(String rest) {
        renewLoan.execute(new RenewLoanRequest(oneWord(rest, "renew C1")), presenter);
        return presenter.text();
    }

    private String reserve(String rest) {
        String[] words = twoWords(rest, "reserve Mary The Hobbit");
        reserveBook.execute(new ReserveBookRequest(words[0], words[1]), presenter);
        return presenter.text();
    }

    private String pay(String rest) {
        payFine.execute(new PayFineRequest(oneWord(rest, "pay Jane")), presenter);
        return presenter.text();
    }

    // ---- the library as it stands: the presenter's lines ----

    private String copies() {
        listCopies.execute(presenter);
        return presenter.text();
    }

    private String members() {
        listMembers.execute(presenter);
        return presenter.text();
    }

    private String reservations() {
        listReservations.execute(presenter);
        return presenter.text();
    }

    // ---- the catalogue: the same shape, the other context ----

    private String add(String rest) {
        addBook.execute(new AddBookRequest(oneWord(rest, "add 978-0-452-28424-1")), presenter);
        return presenter.text();
    }

    private String search(String words) {
        searchCatalogue.execute(new SearchCatalogueRequest(words), presenter);
        return presenter.text();
    }

    private String reset() {
        reset.run();
        return "The library is back in its starting state.";
    }

    private String help() {
        return """
                day 2 March              set the date (the year is 2026)
                borrow John C1           a member borrows a copy
                return C1                a copy comes back
                renew C1                 the loan of a copy is renewed
                reserve Mary The Hobbit  a member reserves a book
                pay Jane                 a member pays what they owe
                copies, members, reservations
                add 978-0-452-28424-1    a book joins the catalogue, by its ISBN
                search Orwell            the catalogue, by words of a title or an author
                catalogue                the whole catalogue
                reset, help, quit""";
    }

    // ---- the shape of a line ----

    private static String oneWord(String rest, String example) {
        if (rest.isEmpty() || rest.contains(" ")) {
            throw new IllegalArgumentException("Write it as in \"" + example + "\".");
        }
        return rest;
    }

    private static String[] twoWords(String rest, String example) {
        String[] words = rest.split("\\s+", 2);
        if (words.length < 2) {
            throw new IllegalArgumentException("Write it as in \"" + example + "\".");
        }
        return words;
    }
}
