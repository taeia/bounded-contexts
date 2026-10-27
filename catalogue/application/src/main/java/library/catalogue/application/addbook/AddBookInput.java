package library.catalogue.application.addbook;

/**
 * The input boundary of the use case: what a driver holds. The request says which
 * ISBN; the output is the presenter that will be told what was catalogued. A refused
 * request ends in a {@code Refusal} and the output hears nothing.
 */
public interface AddBookInput {

    void execute(AddBookRequest request, AddBookOutput output);
}
