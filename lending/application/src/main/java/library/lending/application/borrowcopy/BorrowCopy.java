package library.lending.application.borrowcopy;

import library.lending.application.ChangeSignal;
import library.lending.application.Names;
import library.lending.application.Refusal;
import library.lending.domain.CopyRepository;
import library.lending.domain.Lending;
import library.lending.domain.MemberRepository;
import library.lending.model.Copy;
import library.lending.model.LibraryException;
import library.lending.model.Loan;
import library.lending.model.Member;

/**
 * Borrow a copy. A use case: the first thing Chapter 0 says the library does, as one
 * class, and the class behind the {@link BorrowCopyInput} boundary. It does what
 * Chapter 8's {@code LoanService.borrow} did, in the same four steps: it turns the
 * names in the request into aggregates, asks {@link Lending} to do the work and turns
 * a refusal from the model into a {@link Refusal} at the boundary, rings the
 * {@link ChangeSignal}, and hands the facts the sentence needs to whoever is on the
 * other side of the {@link BorrowCopyOutput} boundary. It returns nothing: the
 * response goes out, it does not come back. The rules are in the aggregates and the
 * saving is done by the domain service that changed them, as before.
 */
public class BorrowCopy implements BorrowCopyInput {

    private final Lending lending;
    private final MemberRepository members;
    private final CopyRepository copies;
    private final ChangeSignal signal;

    public BorrowCopy(Lending lending, MemberRepository members, CopyRepository copies, ChangeSignal signal) {
        this.lending = lending;
        this.members = members;
        this.copies = copies;
        this.signal = signal;
    }

    @Override
    public void execute(BorrowCopyRequest request, BorrowCopyOutput output) {
        Member member = Names.member(members, request.member());
        Copy copy = Names.copy(copies, request.copy());
        Loan loan;
        try {
            loan = lending.borrow(member, copy);
        } catch (LibraryException e) {
            throw new Refusal(e.getMessage());
        }
        signal.changed();
        output.present(new BorrowCopyResponse(member.name(), copy.number(), copy.book().title(), loan.dueDate()));
    }
}
