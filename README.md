# Bounded Contexts

Companion repository for Chapter 11 of [*Domain-Centric Architecture*](https://github.com/taeia/domain-centric-architecture): the library in two bounded contexts, lending and catalogue, each with its own model of a book, and an Anti-Corruption Layer between the catalogue and an external ISBN service.

The lending context is Chapter 10's inside, moved under one directory and one package prefix, `library.lending`, and not changed otherwise: the same aggregates, domain services, nine use cases, in-memory adapters, and seventeen scenarios, and the same sentences. Its `Book` has an ISBN and a title, which is all a copy, a queue, and a sentence need. The catalogue context is new: its `Book` has an `Isbn` that checks its own check digit, a title, and authors, and the catalogue does two things, `AddBook`, which catalogues a book by its ISBN, and `SearchCatalogue`, which finds books by words of a title or an author's name. The two `Book` classes live in two modules that cannot see each other. When a cataloguer types an ISBN, the catalogue asks its `IsbnLookup` port what book that is. Behind the port is Babel, a stand-in for a vendor's ISBN service with a model of its own, thirteen bare digits, contributors surname first, a subtitle, a year; and between the port and Babel is the Anti-Corruption Layer, `BabelLookup` and `BabelTranslator`, which answers in the catalogue's terms and keeps Babel's out. Nothing in either context imports `com.babel`; only the layer and the configurators that build it do.

The poms are the rule, as in Chapters 9 and 10. Under `lending/`, `lending-model` lists nothing, `lending-domain` lists the model, `lending-application` lists both, and `lending-inmemory` lists all three. Under `catalogue/`, `catalogue-model` lists nothing, `catalogue-application` lists the model, `catalogue-inmemory` lists both, and `catalogue-babel`, the Anti-Corruption Layer, lists both and `babel`. No module of one context lists a module of the other. On the outside, `presenters` lists both use-case circles, and the drivers and the scenarios list what their configurators wire.

The lending rules and scenarios are described in [Chapter 0](https://github.com/taeia/domain-centric-architecture/blob/main/chapters/00-the-library.md). The catalogue's rules and scenarios, and the chapter about this code, are in [Chapter 11](https://github.com/taeia/domain-centric-architecture/blob/main/chapters/11-bounded-contexts.md).

## Running

You need Java 25 and Maven. JavaFX is a Maven dependency, so nothing else has to be installed.

```
mvn install
mvn -pl desktop javafx:run
```

The first command builds all fourteen modules and runs the tests. The second starts the window from the desktop module. Repeat the first command after changing anything outside `desktop`.

A window opens with the library in its starting state, and the status bar says that it also listens on `http://localhost:8080`. The upper part is Chapter 10's: copies, members, reservations, notifications, and the date picker that sets the library's idea of today. Below them is the catalogue, three books with their authors, with a search field above it. *Add book* asks for an ISBN and catalogues the book Babel knows under it; try `978-0-452-28424-1`, `978-0-06-085398-3`, or `978-0-618-64015-7`, and then try one of them again. A catalogued book has no copies, so it appears in the catalogue table and not in the Reserve dialog. The *Reset library* button returns both contexts to their starting state.

Now open a terminal and talk to the library from there:

```
curl -X POST "http://localhost:8080/catalogue?isbn=978-0-452-28424-1"
curl "http://localhost:8080/catalogue?q=Orwell"
```

The first answers with the sentence the window would have shown, and the Catalogue table in the window shows Animal Farm without a click. In PowerShell, write `curl.exe`, because `curl` alone is an alias for something else. The lending commands are `POST /borrow?member=John&copy=C1`, `POST /return?copy=C1`, `POST /renew?copy=C1`, `POST /reserve?member=Mary&book=The%20Hobbit`, and `POST /pay?member=Jane`; the lending listings are `GET /copies`, `GET /members`, and `GET /reservations`. The catalogue has `POST /catalogue?isbn=...` and `GET /catalogue`, with or without `?q=`. A refusal answers with status 409 and the reason, and says whether the library or the catalogue refused. There is no `day` over HTTP: the window owns the clock.

To start the window from an IDE, run `library.desktop.Launcher`. Running `MainWindow` directly fails with *JavaFX runtime components are missing*, because JavaFX is on the classpath and the Java launcher checks for the modules whenever the main class extends `Application`. `Launcher` does not extend it, so the check is skipped.

The same library on the command line:

```
mvn -q -pl cli exec:java
```

Type `help` for the commands. The lending commands are Chapter 0's: `day 2 March`, `borrow John C1`, `return C1`, `renew C1`, `reserve Mary The Hobbit`, `pay Jane`, and `copies`, `members`, `reservations`. The catalogue's are `add 978-0-452-28424-1`, `search Orwell`, and `catalogue`. `quit` ends the session. A script runs the long scenario of Chapter 0 from start to finish, and prints what Chapter 10 printed, line for line:

```
mvn -q -pl cli exec:java -Dexec.args=cli/scripts/a-month-at-the-library.txt
```

## Tests

```
mvn test
```

The `scenarios` module has two test classes. `ScenarioTest` is Chapter 10's: one test per scenario of Chapter 0, seventeen, through the input boundaries of the lending context, with the `Recorder` presenter that keeps each response so that an assertion can read a date or an amount out of it. `CatalogueScenarioTest` has one test per catalogue scenario of Chapter 11, eleven, through the input boundaries of the catalogue context, with the real Anti-Corruption Layer on the Babel client, because Babel here is a table; its last scenario, cataloguing adds no copy, wires both contexts side by side.

The unit tests are in the modules they test. `lending-model` has the thirty-eight tests of the aggregates and values, and `lending-domain` the eleven of `Lending` and `Reserving`, both as in Chapter 10. `catalogue-model` has twelve tests of `Isbn` and `Book`, which need no fake because the model asks nothing of anyone. `catalogue-application` has seven tests of the two use cases on a fake catalogue and a fake lookup, so no test in the context knows that Babel exists. `catalogue-babel` has six tests of the translation, run against Babel's own answers. The `cli` module has ten tests of the command line and the `http` module eight of the endpoint, three of each about the catalogue. One hundred and twenty tests in all, of which seventy-eight are Chapter 10's.

## Layout

```
pom.xml                        the parent: two contexts, the foreign system, and the outside
lending/                       the lending context: Chapter 10's inside under its own name
  pom.xml                      lists the four modules of the context
  model/                       lending-model: aggregates, values, LibraryException; package library.lending.model
  domain/                      lending-domain: Lending, Reserving, the repositories, Clock
  application/                 lending-application: nine use cases, Refusal, Notifier, ChangeSignal, Names
  inmemory/                    lending-inmemory: four repositories, SettableClock, InMemoryNotifier, StartingState
catalogue/                     the catalogue context
  pom.xml                      lists the four modules of the context
  model/                       catalogue-model
    src/main/java/library/catalogue/model/
      Book.java                aggregate root: Isbn, title, authors; matches(words)
      Isbn.java                value object: thirteen digits with a check digit, kept as written
      CatalogueException.java  thrown when the model refuses
    src/test/java/library/catalogue/model/
      IsbnTest.java
      BookTest.java
  application/                 catalogue-application: the use-case circle
    src/main/java/library/catalogue/application/
      Catalogue.java           driven port: the books the library knows; all, withIsbn, save
      IsbnLookup.java          driven port: an Isbn in, a Book of this context or null out
      ChangeSignal.java        driven port: the catalogue changed
      Refusal.java             thrown through an input boundary when the catalogue refuses
      addbook/                 AddBook, AddBookInput, AddBookOutput, AddBookRequest (isbn),
                               AddBookResponse (isbn, title, authors)
      searchcatalogue/         SearchCatalogue, its boundaries, SearchCatalogueRequest (words),
                               SearchCatalogueResponse (words, entries), CatalogueEntry
    src/test/java/library/catalogue/application/
      AddBookTest.java         the use cases on the two fakes
      SearchCatalogueTest.java
      FakeCatalogue.java
      FakeIsbnLookup.java
  inmemory/                    catalogue-inmemory
    src/main/java/library/catalogue/inmemory/
      InMemoryCatalogue.java   the Catalogue port, with a list
      StartingCatalogue.java   the three books of Chapter 0, with their authors
  babel/                       catalogue-babel: the Anti-Corruption Layer
    src/main/java/library/catalogue/babel/
      BabelLookup.java         the adapter: implements IsbnLookup with a BabelClient behind it
      BabelTranslator.java     the translator: from a BabelRecord to a Book of the catalogue
    src/test/java/library/catalogue/babel/
      BabelLookupTest.java     the translation, against Babel's own answers
babel/                         the foreign system: a vendor's client, faked
  src/main/java/com/babel/books/
    BabelClient.java           fetch(isbn13): a BabelRecord or null; a table of seven books
    BabelRecord.java           Babel's model: isbn13, title, subtitle, contributors, year, publisher
    BabelException.java        Babel's exception
presenters/                    interface adapters, outside both contexts
  src/main/java/library/presenters/
    TextPresenter.java         ten output boundaries; the sentence or the listing, as text
    TablePresenter.java        five output boundaries; rows of strings for the window
    CopyRow.java               view models: a row each, every field already text
    MemberRow.java
    BookRow.java               a book as lending lists it, for the Reserve dialog
    CatalogueRow.java          a book as the catalogue lists it, for the catalogue table
    ReservationRow.java
http/                          the HTTP endpoint: a controller and a view
  src/main/java/library/http/HttpApi.java
  src/test/java/library/http/HttpApiTest.java
desktop/                       the JavaFX window: a view, with a controller in each dialog
  src/main/java/library/desktop/
    MainWindow.java            start() is the configurator for both contexts and the endpoint
    AddBookDialog.java         the catalogue's dialog
    BorrowDialog.java          the five lending dialogs, Chooser, Loans, Launcher, as in Chapter 10
    ...
cli/                           the command line: a controller and a view
  scripts/a-month-at-the-library.txt
  src/main/java/library/cli/Cli.java
  src/main/java/library/cli/ConsoleNotifier.java
  src/test/java/library/cli/CliTest.java
scenarios/                     the tests
  src/test/java/library/scenarios/
    ScenarioTest.java          one test per scenario of Chapter 0
    CatalogueScenarioTest.java one test per catalogue scenario of Chapter 11
    Recorder.java              a presenter that keeps each response, for the assertions to read
```
