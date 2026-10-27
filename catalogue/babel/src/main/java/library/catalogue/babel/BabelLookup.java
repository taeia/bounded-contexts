package library.catalogue.babel;

import com.babel.books.BabelClient;
import com.babel.books.BabelRecord;
import library.catalogue.application.IsbnLookup;
import library.catalogue.model.Book;
import library.catalogue.model.Isbn;

/**
 * The ISBN lookup, answered by Babel. An Anti-Corruption Layer, and in the hexagon's
 * words a driven adapter whose job is translation: it implements the port the
 * catalogue declared, in the catalogue's terms, and talks to Babel in Babel's. The
 * port takes an {@link Isbn}; Babel wants thirteen bare digits, so the adapter asks
 * the Isbn for them. Babel answers with a {@link BabelRecord} or null; the adapter
 * hands the record to the {@link BabelTranslator} and lets null through, because
 * "nothing known" is the one thing both sides mean the same way. Evans describes the
 * layer as facades, adapters, and translators; Babel's one method needs no facade,
 * so this is the adapter and the next class is the translator, and the layer is the
 * two of them in one module. Nothing from {@code com.babel} gets past this package.
 */
public class BabelLookup implements IsbnLookup {

    private final BabelClient babel;

    public BabelLookup(BabelClient babel) {
        this.babel = babel;
    }

    @Override
    public Book find(Isbn isbn) {
        BabelRecord record = babel.fetch(isbn.digits());
        if (record == null) {
            return null;
        }
        return BabelTranslator.toBook(isbn, record);
    }
}
