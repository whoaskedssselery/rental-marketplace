package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.ValidationResult;
import rentalmarketplace.model.User;
import rentalmarketplace.service.ListingDetails;

public class ListingFormController {
  private static final int TITLE_MAX = 200;
  private static final int DESCRIPTION_MAX = 2000;
  private static final int CATEGORY_MAX = 50;

  public record Values(
      Integer ownerId,
      String title,
      String description,
      BigDecimal pricePerDay,
      String category,
      boolean available) {}

  private final AppContext context;

  @FXML private ComboBox<User> ownerBox;
  @FXML private TextField titleField;
  @FXML private TextArea descriptionArea;
  @FXML private TextField priceField;
  @FXML private ComboBox<String> categoryBox;
  @FXML private CheckBox availableBox;

  public ListingFormController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    ownerBox.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(User user) {
            return user == null ? "" : user.getFullName() + " (" + user.getEmail() + ")";
          }

          @Override
          public User fromString(String text) {
            return null;
          }
        });
    ownerBox.setItems(FXCollections.observableArrayList(context.userService().getAllUsers()));
    categoryBox.setItems(
        FXCollections.observableArrayList(context.listingService().getCategories()));
    availableBox.setSelected(true);
  }

  public void prepareForEdit(ListingDetails details) {
    ownerBox.getItems().stream()
        .filter(user -> user.getId().equals(details.ownerId()))
        .findFirst()
        .ifPresent(ownerBox::setValue);
    titleField.setText(details.title());
    descriptionArea.setText(details.description());
    priceField.setText(details.pricePerDay().toPlainString());
    categoryBox.getEditor().setText(details.category());
    categoryBox.setValue(details.category());
    availableBox.setSelected(details.available());
  }

  public ValidationResult validate() {
    FormValidator validator = new FormValidator();
    validator.selected("Владелец", ownerBox.getValue());
    validator.requiredText("Название", titleField.getText(), TITLE_MAX);
    validator.optionalText("Описание", descriptionArea.getText(), DESCRIPTION_MAX);
    validator.positiveAmount("Цена за день", priceField.getText());
    validator.requiredText("Категория", categoryText(), CATEGORY_MAX);
    return validator.result();
  }

  public Values values() {
    FormValidator parser = new FormValidator();
    BigDecimal price = parser.positiveAmount("Цена за день", priceField.getText());
    String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
    return new Values(
        ownerBox.getValue().getId(),
        titleField.getText().trim(),
        description.isEmpty() ? null : description,
        price,
        categoryText().trim(),
        availableBox.isSelected());
  }

  private String categoryText() {
    String typed = categoryBox.getEditor().getText();
    return typed == null || typed.isBlank() ? categoryBox.getValue() : typed;
  }
}
