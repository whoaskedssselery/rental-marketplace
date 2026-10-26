package rentalmarketplace.fx.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.FormErrors;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.fx.support.ValidationResult;
import rentalmarketplace.model.User;

public class AuthenticationController {
  private final AppContext context;
  private Runnable onAuthenticated = () -> {};
  private boolean registering;
  private boolean pending;
  private FormErrors errors;

  @FXML private VBox formBox;
  @FXML private VBox nameGroup;
  @FXML private Label titleLabel;
  @FXML private Label subtitleLabel;
  @FXML private Label passwordHint;
  @FXML private Label errorLabel;
  @FXML private Label progressLabel;
  @FXML private TextField nameField;
  @FXML private TextField emailField;
  @FXML private PasswordField passwordField;
  @FXML private Button submitButton;
  @FXML private Button switchModeButton;

  public AuthenticationController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    errors = new FormErrors(formBox, () -> errorLabel.setText(""));
    errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
    errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    updateMode();
  }

  public void setOnAuthenticated(Runnable callback) {
    onAuthenticated = callback;
  }

  @FXML
  private void onSwitchMode() {
    if (pending) {
      return;
    }
    registering = !registering;
    passwordField.clear();
    errors.show(new ValidationResult());
    errorLabel.setText("");
    updateMode();
  }

  private void updateMode() {
    titleLabel.setText(registering ? "Создать аккаунт" : "Войти в аккаунт");
    subtitleLabel.setText(
        registering
            ? "Арендуйте вещи и публикуйте свои объекты."
            : "Войдите, чтобы управлять арендой и своими объектами.");
    nameGroup.setVisible(registering);
    nameGroup.setManaged(registering);
    passwordHint.setVisible(registering);
    passwordHint.setManaged(registering);
    submitButton.setText(registering ? "Зарегистрироваться" : "Войти");
    switchModeButton.setText(registering ? "Уже есть аккаунт? Войти" : "Создать аккаунт");
  }

  @FXML
  private void onSubmit() {
    if (pending) {
      return;
    }
    FormValidator validator = new FormValidator();
    if (registering) {
      validator.requiredText("ФИО", nameField.getText(), 150);
    }
    validator.email("Email", emailField.getText(), 150);
    String password = passwordField.getText();
    if (password.isEmpty()) {
      validator.result().add("Пароль", "введите пароль");
    } else if (password.length() > 128 || (registering && password.length() < 15)) {
      validator.result().add("Пароль", "от 15 до 128 символов");
    }
    ValidationResult result = validator.result();
    errors.show(result);
    if (!result.isValid()) {
      return;
    }
    String name = nameField.getText().trim();
    String email = emailField.getText().trim();
    boolean createAccount = registering;
    setPending(true);
    passwordField.clear();
    errorLabel.setText("");
    Task<User> task =
        new Task<>() {
          @Override
          protected User call() {
            context.authenticationService().initialize();
            return createAccount
                ? context.authenticationService().register(name, email, password)
                : context.authenticationService().login(email, password);
          }
        };
    task.setOnSucceeded(
        event -> {
          setPending(false);
          try {
            onAuthenticated.run();
          } catch (RuntimeException error) {
            context.authenticationService().logout();
            errorLabel.setText(UserMessages.of(error));
          }
        });
    task.setOnFailed(
        event -> {
          setPending(false);
          errorLabel.setText(UserMessages.of(task.getException()));
          passwordField.requestFocus();
        });
    Thread worker = new Thread(task, "authentication");
    worker.setDaemon(true);
    worker.start();
  }

  private void setPending(boolean value) {
    pending = value;
    formBox.setDisable(value);
    progressLabel.setVisible(value);
    progressLabel.setText(registering ? "Создаём аккаунт…" : "Входим…");
  }
}
