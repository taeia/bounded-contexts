package library.catalogue.application;

/**
 * Thrown through an input boundary when the catalogue refuses. The message says why:
 * "978-0-547-92822-7 is already in the catalogue.", "Nothing is known about
 * 978-1-234-56789-7.", "hobbit is not an ISBN." The lending context has a Refusal of
 * its own, with the same name, and a driver that holds both contexts catches each by
 * its full name; the chapter says what that costs and what it is worth.
 */
public class Refusal extends RuntimeException {

    public Refusal(String message) {
        super(message);
    }
}
