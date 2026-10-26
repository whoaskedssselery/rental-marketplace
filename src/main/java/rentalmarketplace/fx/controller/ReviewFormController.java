package rentalmarketplace.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.ValidationResult;
import rentalmarketplace.service.ReviewDetails;
import rentalmarketplace.service.ReviewableRequest;

public class ReviewFormController {
  private static final int COMMENT_MAX = 1000;

  public record Values(Integer requestId, int rating, String comment) {}

  private final AppContext context;
  private Integer editedRequestId;

  @FXML private Label requestLabel;
  @FXML private ComboBox<ReviewableRequest> requestBox;
  @FXML private ComboBox<Integer> ratingBox;
  @FXML private TextArea commentArea;
  @FXML private Label hintLabel;

  public ReviewFormController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
    requestBox.setItems(
        FXCollections.observableArrayList(context.reviewService().getReviewableRequests()));
    hintLabel.setText("Отзыв можно оставить на завершенную заявку, по одному на заявку.");
  }

  public void prepareForEdit(ReviewDetails details) {
    editedRequestId = details.requestId();
    requestBox.setVisible(false);
    requestBox.setManaged(false);
    requestLabel.setText(
        "Заявка №"
            + details.requestId()
            + ": "
            + details.listingTitle()
            + ", "
            + details.renterName());
    ratingBox.setValue(details.rating());
    commentArea.setText(details.comment());
    hintLabel.setText("Можно изменить только оценку и комментарий.");
  }

  public ValidationResult validate() {
    FormValidator validator = new FormValidator();
    if (editedRequestId == null) {
      validator.selected("Заявка", requestBox.getValue());
    }
    validator.selected("Оценка", ratingBox.getValue());
    validator.optionalText("Комментарий", commentArea.getText(), COMMENT_MAX);
    return validator.result();
  }

  public Values values() {
    Integer requestId =
        editedRequestId != null ? editedRequestId : requestBox.getValue().requestId();
    return new Values(requestId, ratingBox.getValue(), commentArea.getText());
  }
}
