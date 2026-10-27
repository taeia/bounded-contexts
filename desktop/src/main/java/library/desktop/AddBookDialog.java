package library.desktop;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import library.catalogue.application.Refusal;
import library.catalogue.application.addbook.AddBookInput;
import library.catalogue.application.addbook.AddBookRequest;
import library.presenters.TextPresenter;

/**
 * The Add book button's dialog, and the one dialog of the catalogue context. A view
 * and a controller in one class: a field for the ISBN, a request built from what was
 * typed, one use case called, and the sentence the text presenter made of the
 * response kept for the status bar. It imports the catalogue's Refusal and nothing
 * of the lending context, which is why it can call it by its short name.
 */
public class AddBookDialog extends Stage {

    public String result = "";

    public AddBookDialog(AddBookInput addBook, TextPresenter sentences) {
        setTitle("Add a book to the catalogue");
        initModality(Modality.APPLICATION_MODAL);

        TextField isbnField = new TextField();
        isbnField.setPromptText("978-0-452-28424-1");
        isbnField.setPrefColumnCount(18);
        Label message = new Label();
        message.setStyle("-fx-text-fill: red;");

        Button add = new Button("Add");
        add.setOnAction(e -> {
            String isbn = isbnField.getText().strip();
            if (isbn.isEmpty()) {
                message.setText("Type an ISBN.");
                return;
            }
            try {
                addBook.execute(new AddBookRequest(isbn), sentences);
                result = sentences.text();
                close();
            } catch (Refusal ex) {
                message.setText(ex.getMessage());
            }
        });
        add.setDefaultButton(true);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("ISBN"), 0, 0);
        grid.add(isbnField, 1, 0);
        grid.add(message, 0, 1, 2, 1);
        grid.add(new HBox(10, add, cancel), 0, 2, 2, 1);

        setScene(new Scene(grid));
    }
}
