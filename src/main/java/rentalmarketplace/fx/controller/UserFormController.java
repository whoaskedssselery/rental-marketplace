package rentalmarketplace.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.ValidationResult;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;

public class UserFormController {
  private static final int NAME_MAX = 150;
  private static final int EMAIL_MAX = 150;
  private static final int PHONE_MAX = 30;

  public record Values(String fullName, String email, String phone, UserRole role) {}

  @FXML private TextField nameField;
  @FXML private TextField emailField;
  @FXML private TextField phoneField;
  @FXML private ComboBox<UserRole> roleBox;

  @FXML
  private void initialize() {
    roleBox.setItems(FXCollections.observableArrayList(UserRole.values()));
    roleBox.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(UserRole role) {
            return Labels.role(role);
          }

          @Override
          public UserRole fromString(String text) {
            return null;
          }
        });
  }

  public void prepareForEdit(User user) {
    nameField.setText(user.getFullName());
    emailField.setText(user.getEmail());
    phoneField.setText(user.getPhone());
    roleBox.setValue(user.getRole());
  }

  public void lockRole() {
    roleBox.setDisable(true);
  }

  public ValidationResult validate() {
    FormValidator validator = new FormValidator();
    validator.requiredText("ФИО", nameField.getText(), NAME_MAX);
    validator.email("Email", emailField.getText(), EMAIL_MAX);
    validator.optionalText("Телефон", phoneField.getText(), PHONE_MAX);
    validator.selected("Роль", roleBox.getValue());
    return validator.result();
  }

  public Values values() {
    String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
    return new Values(
        nameField.getText().trim(),
        emailField.getText().trim(),
        phone.isEmpty() ? null : phone,
        roleBox.getValue());
  }
}
