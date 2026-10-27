package library.presenters;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import library.catalogue.application.addbook.AddBookOutput;
import library.catalogue.application.addbook.AddBookResponse;
import library.catalogue.application.searchcatalogue.CatalogueEntry;
import library.catalogue.application.searchcatalogue.SearchCatalogueOutput;
import library.catalogue.application.searchcatalogue.SearchCatalogueResponse;
import library.lending.application.borrowcopy.BorrowCopyOutput;
import library.lending.application.borrowcopy.BorrowCopyResponse;
import library.lending.application.listcopies.CopySummary;
import library.lending.application.listcopies.ListCopiesOutput;
import library.lending.application.listcopies.ListCopiesResponse;
import library.lending.application.listmembers.ListMembersOutput;
import library.lending.application.listmembers.ListMembersResponse;
import library.lending.application.listmembers.MemberSummary;
import library.lending.application.listreservations.ListReservationsOutput;
import library.lending.application.listreservations.ListReservationsResponse;
import library.lending.application.listreservations.ReservationSummary;
import library.lending.application.payfine.PayFineOutput;
import library.lending.application.payfine.PayFineResponse;
import library.lending.application.renewloan.RenewLoanOutput;
import library.lending.application.renewloan.RenewLoanResponse;
import library.lending.application.reservebook.ReserveBookOutput;
import library.lending.application.reservebook.ReserveBookResponse;
import library.lending.application.returncopy.ReturnCopyOutput;
import library.lending.application.returncopy.ReturnCopyResponse;

/**
 * The library in words. A presenter: it implements the output boundaries of the five
 * lending commands and of three lending listings, and now of the catalogue's two use
 * cases, and turns each response into the sentence of Chapter 0, "John borrowed C1
 * of The Hobbit. It is due on 16 March.", or of Chapter 11, "Animal Farm by George
 * Orwell is now in the catalogue.", or into the lines of a listing, one per row,
 * joined with newlines and with no newline at the end. It is the one class that
 * speaks both contexts' languages, and it can, because it is outside both: it
 * implements interfaces from two use-case circles that cannot see each other, and
 * nothing in it crosses from one to the other. The three drivers hold one of these
 * and read {@link #text()} after the use case has run. The view model is a string,
 * kept from one call to the next.
 *
 * Dates are written as "16 March" here and nowhere else. The lending books listing
 * has no text form, because no driver prints it.
 */
public class TextPresenter implements BorrowCopyOutput, ReturnCopyOutput, RenewLoanOutput,
        ReserveBookOutput, PayFineOutput, ListCopiesOutput, ListMembersOutput, ListReservationsOutput,
        AddBookOutput, SearchCatalogueOutput {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);

    private String text = "";

    /**
     * What the last use case had to say, as text.
     */
    public String text() {
        return text;
    }

    // ---- the five sentences ----

    @Override
    public void present(BorrowCopyResponse response) {
        text = response.member() + " borrowed " + response.copy() + " of " + response.book()
                + ". It is due on " + response.dueDate().format(DAY) + ".";
    }

    @Override
    public void present(ReturnCopyResponse response) {
        String result;
        if (response.daysLate() > 0) {
            result = response.member() + " returned " + response.copy() + " " + response.daysLate()
                    + " days late and now owes " + response.debt() + ".";
        } else {
            result = response.member() + " returned " + response.copy() + " on time.";
        }
        if (response.heldFor() != null) {
            result += " " + response.copy() + " is on hold for " + response.heldFor() + ", who has been notified.";
        }
        text = result;
    }

    @Override
    public void present(RenewLoanResponse response) {
        text = response.member() + " renewed the loan of " + response.copy()
                + ". It is now due on " + response.dueDate().format(DAY) + ".";
    }

    @Override
    public void present(ReserveBookResponse response) {
        text = response.member() + " reserved " + response.book()
                + ". Position in the queue: " + response.position() + ".";
    }

    @Override
    public void present(PayFineResponse response) {
        text = response.member() + " paid " + response.paid() + " and owes nothing now.";
    }

    // ---- the three listings ----

    @Override
    public void present(ListCopiesResponse response) {
        StringBuilder result = new StringBuilder();
        for (CopySummary copy : response.copies()) {
            result.append(copy.number()).append("  ").append(copy.book()).append(": ");
            switch (copy.status()) {
                case ON_HOLD -> result.append("on hold for ").append(copy.heldFor());
                case ON_LOAN -> result.append("on loan to ").append(copy.borrower())
                        .append(", due ").append(copy.dueDate().format(DAY));
                case AVAILABLE -> result.append("available");
            }
            result.append('\n');
        }
        text = result.toString().stripTrailing();
    }

    @Override
    public void present(ListMembersResponse response) {
        StringBuilder result = new StringBuilder();
        for (MemberSummary member : response.members()) {
            result.append(member.name()).append(": ").append(member.loans())
                    .append(member.loans() == 1 ? " loan" : " loans")
                    .append(", owes ").append(member.debt()).append('\n');
        }
        text = result.toString().stripTrailing();
    }

    @Override
    public void present(ListReservationsResponse response) {
        if (response.reservations().isEmpty()) {
            text = "No reservations.";
            return;
        }
        StringBuilder result = new StringBuilder();
        for (ReservationSummary reservation : response.reservations()) {
            result.append(reservation.book()).append(": ").append(reservation.member())
                    .append(", position ").append(reservation.position()).append('\n');
        }
        text = result.toString().stripTrailing();
    }

    // ---- the catalogue: one sentence and one listing ----

    @Override
    public void present(AddBookResponse response) {
        text = response.title() + by(response.authors()) + " is now in the catalogue.";
    }

    @Override
    public void present(SearchCatalogueResponse response) {
        if (response.entries().isEmpty()) {
            text = response.words().isBlank()
                    ? "The catalogue is empty."
                    : "Nothing in the catalogue matches " + response.words().strip() + ".";
            return;
        }
        StringBuilder result = new StringBuilder();
        for (CatalogueEntry entry : response.entries()) {
            result.append(entry.isbn()).append("  ").append(entry.title()).append(by(entry.authors())).append('\n');
        }
        text = result.toString().stripTrailing();
    }

    /**
     * " by George Orwell", " by Terry Pratchett and Neil Gaiman", or nothing for a
     * book nobody signed. How two authors are joined is this presenter's decision.
     */
    private static String by(List<String> authors) {
        if (authors.isEmpty()) {
            return "";
        }
        if (authors.size() == 1) {
            return " by " + authors.get(0);
        }
        return " by " + String.join(", ", authors.subList(0, authors.size() - 1))
                + " and " + authors.get(authors.size() - 1);
    }
}
