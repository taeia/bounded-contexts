package library.desktop;

import java.io.IOException;
import java.util.Locale;

import com.babel.books.BabelClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import library.catalogue.application.addbook.AddBook;
import library.catalogue.application.addbook.AddBookInput;
import library.catalogue.application.searchcatalogue.SearchCatalogue;
import library.catalogue.application.searchcatalogue.SearchCatalogueInput;
import library.catalogue.application.searchcatalogue.SearchCatalogueRequest;
import library.catalogue.babel.BabelLookup;
import library.catalogue.inmemory.InMemoryCatalogue;
import library.catalogue.inmemory.StartingCatalogue;
import library.http.HttpApi;
import library.lending.application.borrowcopy.BorrowCopy;
import library.lending.application.borrowcopy.BorrowCopyInput;
import library.lending.application.listbooks.ListBooks;
import library.lending.application.listbooks.ListBooksInput;
import library.lending.application.listcopies.ListCopies;
import library.lending.application.listcopies.ListCopiesInput;
import library.lending.application.listmembers.ListMembers;
import library.lending.application.listmembers.ListMembersInput;
import library.lending.application.listreservations.ListReservations;
import library.lending.application.listreservations.ListReservationsInput;
import library.lending.application.payfine.PayFine;
import library.lending.application.payfine.PayFineInput;
import library.lending.application.renewloan.RenewLoan;
import library.lending.application.renewloan.RenewLoanInput;
import library.lending.application.reservebook.ReserveBook;
import library.lending.application.reservebook.ReserveBookInput;
import library.lending.application.returncopy.ReturnCopy;
import library.lending.application.returncopy.ReturnCopyInput;
import library.lending.domain.Lending;
import library.lending.domain.Reserving;
import library.lending.inmemory.InMemoryBookRepository;
import library.lending.inmemory.InMemoryCopyRepository;
import library.lending.inmemory.InMemoryMemberRepository;
import library.lending.inmemory.InMemoryNotifier;
import library.lending.inmemory.InMemoryQueueRepository;
import library.lending.inmemory.SettableClock;
import library.lending.inmemory.StartingState;
import library.presenters.CatalogueRow;
import library.presenters.CopyRow;
import library.presenters.MemberRow;
import library.presenters.ReservationRow;
import library.presenters.TablePresenter;
import library.presenters.TextPresenter;

/**
 * The window. Martin's view, a humble one: it shows rows and sentences that two
 * presenters made and decides nothing about them, and its buttons open dialogs that
 * are the controllers. It shows both contexts, the lending tables above and the
 * catalogue below, and it is where the two meet for the person at the desk: the same
 * toolbar, the same status bar, the same Reset. {@link #start} is the configurator
 * for this program and for the endpoint it starts beside itself; {@link #refresh}
 * runs the listing use cases of both contexts again whenever either change signal
 * rings.
 */
public class MainWindow extends Application {

    private static final int PORT = 8080;

    private BorrowCopyInput borrowCopy;
    private ReturnCopyInput returnCopy;
    private RenewLoanInput renewLoan;
    private ReserveBookInput reserveBook;
    private PayFineInput payFine;
    private ListCopiesInput listCopies;
    private ListMembersInput listMembers;
    private ListBooksInput listBooks;
    private ListReservationsInput listReservations;
    private AddBookInput addBook;
    private SearchCatalogueInput searchCatalogue;
    private final TablePresenter tables = new TablePresenter();
    private final TextPresenter sentences = new TextPresenter();
    private InMemoryNotifier notifier;
    private HttpApi api;

    private final ObservableList<CopyRow> copies = FXCollections.observableArrayList();
    private final ObservableList<MemberRow> members = FXCollections.observableArrayList();
    private final ObservableList<ReservationRow> reservations = FXCollections.observableArrayList();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();
    private final ObservableList<CatalogueRow> catalogue = FXCollections.observableArrayList();
    private TextField searchField;
    private Label status;

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // The configurator for this program: the lending context first, as in Chapter
        // 10, its driven adapters, then the domain services that use them, then the
        // nine use cases, which the rest of the window holds by their input
        // boundaries. Then the catalogue context: its in-memory catalogue, the
        // Anti-Corruption Layer on a Babel client behind its lookup port, and its two
        // use cases. The two contexts are built one after the other and handed
        // nothing of each other. One of the driven adapters is the window itself: each
        // context has a change signal of its own, and both are answered with the same
        // lambda, which refreshes every table on the window's own thread whatever
        // thread it was rung from. The two presenters are fields: the window keeps them
        // for its dialogs and reads them after every use case it runs.
        InMemoryBookRepository bookRepository = new InMemoryBookRepository();
        InMemoryCopyRepository copyRepository = new InMemoryCopyRepository();
        InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository();
        SettableClock clock = new SettableClock();
        notifier = new InMemoryNotifier();
        Runnable changed = () -> Platform.runLater(this::refresh);
        library.lending.application.ChangeSignal lendingSignal = changed::run;
        library.catalogue.application.ChangeSignal catalogueSignal = changed::run;
        Lending lending = new Lending(memberRepository, copyRepository, queueRepository, clock);
        Reserving reserving = new Reserving(copyRepository, queueRepository);
        borrowCopy = new BorrowCopy(lending, memberRepository, copyRepository, lendingSignal);
        returnCopy = new ReturnCopy(lending, memberRepository, copyRepository, notifier, lendingSignal);
        renewLoan = new RenewLoan(lending, memberRepository, copyRepository, lendingSignal);
        reserveBook = new ReserveBook(reserving, bookRepository, memberRepository, queueRepository, lendingSignal);
        payFine = new PayFine(memberRepository, lendingSignal);
        listCopies = new ListCopies(copyRepository, memberRepository);
        listMembers = new ListMembers(memberRepository);
        listBooks = new ListBooks(bookRepository);
        listReservations = new ListReservations(queueRepository);

        InMemoryCatalogue catalogueRepository = new InMemoryCatalogue();
        BabelLookup lookup = new BabelLookup(new BabelClient());
        addBook = new AddBook(catalogueRepository, lookup, catalogueSignal);
        searchCatalogue = new SearchCatalogue(catalogueRepository);

        // The same configurator starts the second driver of this program: the HTTP
        // endpoint, on ten of the same use cases. Every request is answered on the JavaFX
        // thread, so the window and the terminal never reach a use case at the same
        // instant, and the signal a request rings refreshes this window.
        api = new HttpApi(borrowCopy, returnCopy, renewLoan, reserveBook, payFine,
                listCopies, listMembers, listReservations, addBook, searchCatalogue);
        String listening;
        try {
            api.start(PORT, Platform::runLater);
            listening = "It also listens on http://localhost:" + PORT + ".";
        } catch (IOException e) {
            api = null;
            listening = "Port " + PORT + " is taken, so it listens to this window only.";
        }

        // The starting state is loaded where the repositories are built, one half per
        // context. No use case owns the whole library, so none of them can offer a
        // reset(). The mailbox is this window's, so the window empties it.
        Runnable resetLibrary = () -> {
            StartingState.load(bookRepository, copyRepository, memberRepository, queueRepository);
            StartingCatalogue.load(catalogueRepository);
            notifier.clear();
        };
        resetLibrary.run();

        DatePicker datePicker = new DatePicker(clock.today());
        datePicker.setOnAction(e -> {
            clock.set(datePicker.getValue());
            refresh();
        });

        Button borrow = new Button("Borrow");
        borrow.setOnAction(e -> {
            BorrowDialog dialog = new BorrowDialog(borrowCopy, listMembers, listCopies, tables, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button returnButton = new Button("Return");
        returnButton.setOnAction(e -> {
            ReturnDialog dialog = new ReturnDialog(returnCopy, listCopies, tables, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button renew = new Button("Renew");
        renew.setOnAction(e -> {
            RenewDialog dialog = new RenewDialog(renewLoan, listCopies, tables, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button reserve = new Button("Reserve");
        reserve.setOnAction(e -> {
            ReserveDialog dialog = new ReserveDialog(reserveBook, listMembers, listBooks, tables, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button pay = new Button("Pay");
        pay.setOnAction(e -> {
            PayDialog dialog = new PayDialog(payFine, listMembers, tables, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button add = new Button("Add book");
        add.setOnAction(e -> {
            AddBookDialog dialog = new AddBookDialog(addBook, sentences);
            dialog.showAndWait();
            done(dialog.result);
        });
        Button reset = new Button("Reset library");
        reset.setOnAction(e -> {
            resetLibrary.run();
            status.setText("The library is back in its starting state.");
            refresh();
        });

        HBox toolbar = new HBox(10, new Label("Today:"), datePicker, borrow, returnButton, renew, reserve, pay,
                add, reset);
        toolbar.setPadding(new Insets(10));

        // The tables: a column per field of a row, and a row is strings the presenter
        // made. Chapter 9's columns formatted a date and switched on a status here;
        // these do neither.
        TableView<CopyRow> copyTable = new TableView<>(copies);
        TableColumn<CopyRow, String> copyColumn = new TableColumn<>("Copy");
        copyColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().number()));
        TableColumn<CopyRow, String> bookColumn = new TableColumn<>("Book");
        bookColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().book()));
        bookColumn.setPrefWidth(160);
        TableColumn<CopyRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().status()));
        statusColumn.setPrefWidth(120);
        TableColumn<CopyRow, String> borrowerColumn = new TableColumn<>("Borrower");
        borrowerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().borrower()));
        TableColumn<CopyRow, String> dueColumn = new TableColumn<>("Due");
        dueColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().due()));
        copyTable.getColumns().add(copyColumn);
        copyTable.getColumns().add(bookColumn);
        copyTable.getColumns().add(statusColumn);
        copyTable.getColumns().add(borrowerColumn);
        copyTable.getColumns().add(dueColumn);

        TableView<MemberRow> memberTable = new TableView<>(members);
        TableColumn<MemberRow, String> nameColumn = new TableColumn<>("Member");
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<MemberRow, String> loansColumn = new TableColumn<>("Loans");
        loansColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().loans()));
        TableColumn<MemberRow, String> debtColumn = new TableColumn<>("Debt");
        debtColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().debt()));
        memberTable.getColumns().add(nameColumn);
        memberTable.getColumns().add(loansColumn);
        memberTable.getColumns().add(debtColumn);

        TableView<ReservationRow> reservationTable = new TableView<>(reservations);
        TableColumn<ReservationRow, String> reservedBookColumn = new TableColumn<>("Book");
        reservedBookColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().book()));
        reservedBookColumn.setPrefWidth(160);
        TableColumn<ReservationRow, String> reservedByColumn = new TableColumn<>("Member");
        reservedByColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().member()));
        TableColumn<ReservationRow, String> positionColumn = new TableColumn<>("Position");
        positionColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().position()));
        reservationTable.getColumns().add(reservedBookColumn);
        reservationTable.getColumns().add(reservedByColumn);
        reservationTable.getColumns().add(positionColumn);

        ListView<String> notificationList = new ListView<>(notifications);

        // The catalogue table: the other context's book, as its presenter rows it. A
        // search field above it; what is typed there goes into the request of the
        // search use case, and the table shows what came back.
        TableView<CatalogueRow> catalogueTable = new TableView<>(catalogue);
        TableColumn<CatalogueRow, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isbn()));
        isbnColumn.setPrefWidth(150);
        TableColumn<CatalogueRow, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().title()));
        titleColumn.setPrefWidth(300);
        TableColumn<CatalogueRow, String> authorsColumn = new TableColumn<>("Authors");
        authorsColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().authors()));
        authorsColumn.setPrefWidth(300);
        catalogueTable.getColumns().add(isbnColumn);
        catalogueTable.getColumns().add(titleColumn);
        catalogueTable.getColumns().add(authorsColumn);
        searchField = new TextField();
        searchField.setPromptText("Search by title or author");
        searchField.setPrefColumnCount(24);
        searchField.textProperty().addListener((text, before, after) -> refreshCatalogue());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(titled("Copies", copyTable), 0, 0);
        grid.add(titled("Members", memberTable), 1, 0);
        grid.add(titled("Reservations", reservationTable), 0, 1);
        grid.add(titled("Notifications", notificationList), 1, 1);
        grid.add(titled("Catalogue", searchField, catalogueTable), 0, 2, 2, 1);

        status = new Label("Welcome to the library. " + listening);
        status.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(grid);
        root.setBottom(status);

        refresh();
        stage.setTitle("Library");
        stage.setScene(new Scene(root, 1000, 800));
        stage.show();
    }

    @Override
    public void stop() {
        if (api != null) {
            api.stop();
        }
    }

    /**
     * A dialog's sentence goes to the status bar. The tables are not refreshed here:
     * the command the dialog ran has rung a signal, and the signal refreshes them.
     */
    private void done(String result) {
        status.setText(result);
    }

    private VBox titled(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(5, label, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    private VBox titled(String title, Node header, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        HBox heading = new HBox(10, label, header);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(5, heading, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    /**
     * Asks the library everything again. This is the whole of what the window does
     * when a signal rings, whoever rang it: a signal says nothing about what changed,
     * or in which context, so the window does not guess. Three lending listings and
     * the catalogue search run, the table presenter turns their responses into rows,
     * and the rows go into the lists the tables watch.
     */
    private void refresh() {
        listCopies.execute(tables);
        listMembers.execute(tables);
        listReservations.execute(tables);
        copies.setAll(tables.copies());
        members.setAll(tables.members());
        reservations.setAll(tables.reservations());
        notifications.setAll(notifier.sent());
        refreshCatalogue();
    }

    private void refreshCatalogue() {
        searchCatalogue.execute(new SearchCatalogueRequest(searchField.getText()), tables);
        catalogue.setAll(tables.catalogue());
    }
}
