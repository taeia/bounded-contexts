package library.catalogue.application.addbook;

/**
 * The request model: an ISBN as the cataloguer typed it, with or without hyphens,
 * which is what a line of text, a dialog, or a URL can carry. Whether it is an ISBN
 * at all is for the model to say.
 */
public record AddBookRequest(String isbn) {
}
