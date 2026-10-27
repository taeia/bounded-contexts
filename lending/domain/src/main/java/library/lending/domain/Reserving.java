package library.lending.domain;

import library.lending.model.Book;
import library.lending.model.Copy;
import library.lending.model.LibraryException;
import library.lending.model.Member;
import library.lending.model.Queue;
import library.lending.model.Reservation;

/**
 * Reserving a book. A domain service: whether a book can be reserved depends on its
 * copies and on the member's loans, which belong to other aggregates than the queue
 * the reservation joins.
 */
public class Reserving {

    private final CopyRepository copies;
    private final QueueRepository queues;

    public Reserving(CopyRepository copies, QueueRepository queues) {
        this.copies = copies;
        this.queues = queues;
    }

    /**
     * Returns the new reservation, which joined the end of the queue.
     */
    public Reservation reserve(Member member, Book book) {
        for (Copy copy : copies.ofBook(book)) {
            if (copy.isAvailable()) {
                throw new LibraryException("A copy of " + book.title() + " is available.");
            }
        }
        if (member.hasOnLoan(book)) {
            throw new LibraryException(member.name() + " has a copy of " + book.title() + " on loan.");
        }
        Queue queue = queues.of(book);
        Reservation reservation = queue.join(member);
        queues.save(queue);
        return reservation;
    }
}
