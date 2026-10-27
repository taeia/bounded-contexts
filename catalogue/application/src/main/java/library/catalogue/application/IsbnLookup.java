package library.catalogue.application;

import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * What the catalogue asks when a cataloguer gives it an ISBN and nothing else: what
 * book is that. A driven port. It is written in this context's terms and in no
 * others: an {@link Isbn} in, a {@link Book} of this context out, or null when nobody
 * knows the number. It says nothing about who answers, what they call a book, or how
 * they spell an author's name, because the use case that calls it must not have to
 * know. The adapter that implements it with a foreign service behind it, and
 * translates on the way through, is what Evans calls an Anti-Corruption Layer.
 */
public interface IsbnLookup {

    Book find(Isbn isbn);
}
