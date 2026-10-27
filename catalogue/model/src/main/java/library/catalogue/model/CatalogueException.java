package library.catalogue.model;

/**
 * Thrown when the catalogue's model refuses: a number that is not an ISBN, a book
 * without a title. The message says why. The lending context has an exception of
 * its own for the same job, and the two never meet, because no class sees both.
 */
public class CatalogueException extends RuntimeException {

    public CatalogueException(String message) {
        super(message);
    }
}
