package library.presenters;

/**
 * One book as the catalogue table shows it. The view model of a catalogue entry: the
 * ISBN as it was written, the title, and the authors already joined into one string,
 * because a column shows one string and how authors are separated is a decision.
 */
public record CatalogueRow(String isbn, String title, String authors) {
}
