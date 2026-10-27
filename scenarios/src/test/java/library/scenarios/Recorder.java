package library.scenarios;

import library.catalogue.application.addbook.AddBookOutput;
import library.catalogue.application.addbook.AddBookResponse;
import library.catalogue.application.searchcatalogue.SearchCatalogueOutput;
import library.catalogue.application.searchcatalogue.SearchCatalogueResponse;
import library.lending.application.borrowcopy.BorrowCopyOutput;
import library.lending.application.borrowcopy.BorrowCopyResponse;
import library.lending.application.listbooks.ListBooksOutput;
import library.lending.application.listbooks.ListBooksResponse;
import library.lending.application.listcopies.ListCopiesOutput;
import library.lending.application.listcopies.ListCopiesResponse;
import library.lending.application.listmembers.ListMembersOutput;
import library.lending.application.listmembers.ListMembersResponse;
import library.lending.application.listreservations.ListReservationsOutput;
import library.lending.application.listreservations.ListReservationsResponse;
import library.lending.application.payfine.PayFineOutput;
import library.lending.application.payfine.PayFineResponse;
import library.lending.application.renewloan.RenewLoanOutput;
import library.lending.application.renewloan.RenewLoanResponse;
import library.lending.application.reservebook.ReserveBookOutput;
import library.lending.application.reservebook.ReserveBookResponse;
import library.lending.application.returncopy.ReturnCopyOutput;
import library.lending.application.returncopy.ReturnCopyResponse;

/**
 * A presenter that remembers. The scenarios are a driver, and a driver needs a
 * presenter on the output side of every use case it runs; this one implements the
 * nine output boundaries of the lending context and the two of the catalogue, and
 * keeps the last response of each kind, so that a test can read a due date, a debt,
 * or an author back as a value and compare it. It is the view model of a test: not
 * text, not rows, but the response itself. It lives with the scenarios because no
 * program shows the library this way.
 */
class Recorder implements BorrowCopyOutput, ReturnCopyOutput, RenewLoanOutput, ReserveBookOutput,
        PayFineOutput, ListCopiesOutput, ListMembersOutput, ListBooksOutput, ListReservationsOutput,
        AddBookOutput, SearchCatalogueOutput {

    BorrowCopyResponse borrowed;
    ReturnCopyResponse returned;
    RenewLoanResponse renewed;
    ReserveBookResponse reserved;
    PayFineResponse paid;
    ListCopiesResponse copies;
    ListMembersResponse members;
    ListBooksResponse books;
    ListReservationsResponse reservations;
    AddBookResponse added;
    SearchCatalogueResponse found;

    @Override
    public void present(BorrowCopyResponse response) {
        borrowed = response;
    }

    @Override
    public void present(ReturnCopyResponse response) {
        returned = response;
    }

    @Override
    public void present(RenewLoanResponse response) {
        renewed = response;
    }

    @Override
    public void present(ReserveBookResponse response) {
        reserved = response;
    }

    @Override
    public void present(PayFineResponse response) {
        paid = response;
    }

    @Override
    public void present(ListCopiesResponse response) {
        copies = response;
    }

    @Override
    public void present(ListMembersResponse response) {
        members = response;
    }

    @Override
    public void present(ListBooksResponse response) {
        books = response;
    }

    @Override
    public void present(ListReservationsResponse response) {
        reservations = response;
    }

    @Override
    public void present(AddBookResponse response) {
        added = response;
    }

    @Override
    public void present(SearchCatalogueResponse response) {
        found = response;
    }
}
