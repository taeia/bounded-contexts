package library.catalogue.application.addbook;

/**
 * The output boundary of the use case, implemented by a presenter on the outside.
 */
public interface AddBookOutput {

    void present(AddBookResponse response);
}
