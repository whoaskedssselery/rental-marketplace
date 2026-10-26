package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Window;
import javafx.util.StringConverter;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.FormValidator;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.ValidationResult;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.service.RequestDetails;

public class RequestFormController {
  public record Values(
      Integer listingId, Integer renterId, LocalDate startDate, LocalDate endDate) {}

  private final AppContext context;

  @FXML private ComboBox<Listing> listingBox;
  @FXML private ComboBox<User> renterBox;
  @FXML private DatePicker startPicker;
  @FXML private DatePicker endPicker;
  @FXML private Label totalLabel;

  public RequestFormController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    configureDate(startPicker);
    configureDate(endPicker);
    listingBox.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Listing listing) {
            return listing == null
                ? ""
                : listing.getTitle() + " (" + Labels.money(listing.getPricePerDay()) + "/день)";
          }

          @Override
          public Listing fromString(String text) {
            return null;
          }
        });
    renterBox.setConverter(
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
    renterBox.setItems(FXCollections.observableArrayList(context.userService().getVisibleUsers()));
    if (!context.access().isAdmin()) {
      renterBox.setValue(context.access().currentUser());
      renterBox.setDisable(true);
    }
    listingBox.valueProperty().addListener((o, was, now) -> updateTotal());
    startPicker.valueProperty().addListener((o, was, now) -> updateTotal());
    endPicker.valueProperty().addListener((o, was, now) -> updateTotal());
    startPicker.getEditor().textProperty().addListener((o, was, now) -> updateTotal());
    endPicker.getEditor().textProperty().addListener((o, was, now) -> updateTotal());
    updateTotal();
  }

  private void configureDate(DatePicker picker) {
    picker.setPromptText("дд.мм.гггг");
    picker.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(LocalDate date) {
            return Labels.date(date);
          }

          @Override
          public LocalDate fromString(String text) {
            try {
              return FormValidator.parseDateText(text);
            } catch (DateTimeParseException error) {
              return null;
            }
          }
        });
  }

  public void prepareForCreate() {
    List<Listing> available =
        context.listingService().getAllListings().stream()
            .filter(Listing::isAvailable)
            .filter(
                listing ->
                    context.access().isAdmin()
                        || !listing.getOwnerId().equals(context.access().currentUser().getId()))
            .toList();
    listingBox.setItems(FXCollections.observableArrayList(available));
  }

  public void prepareForEdit(RequestDetails details) {
    listingBox.setItems(
        FXCollections.observableArrayList(context.listingService().getAllListings()));
    listingBox.getItems().stream()
        .filter(listing -> listing.getId().equals(details.listingId()))
        .findFirst()
        .ifPresent(listingBox::setValue);
    renterBox.getItems().stream()
        .filter(user -> user.getId().equals(details.renterId()))
        .findFirst()
        .ifPresent(renterBox::setValue);
    startPicker.setValue(details.startDate());
    endPicker.setValue(details.endDate());
    listingBox.setDisable(true);
    renterBox.setDisable(true);
    if (!context.rentalRequestService().canEdit(details.requestId())) {
      startPicker.setDisable(true);
      endPicker.setDisable(true);
    }
  }

  public ValidationResult validate() {
    FormValidator validator = new FormValidator();
    validator.selected("Объект", listingBox.getValue());
    validator.selected("Арендатор", renterBox.getValue());
    LocalDate start = validator.requiredDateText("Дата начала", startPicker.getEditor().getText());
    LocalDate end = validator.requiredDateText("Дата окончания", endPicker.getEditor().getText());
    validator.endAfterStart("Дата окончания", start, end);
    return validator.result();
  }

  public Values values() {
    return new Values(
        listingBox.getValue().getId(),
        renterBox.getValue().getId(),
        FormValidator.parseDateText(startPicker.getEditor().getText()),
        FormValidator.parseDateText(endPicker.getEditor().getText()));
  }

  private void updateTotal() {
    Listing listing = listingBox.getValue();
    boolean wasError = totalLabel.getStyleClass().contains("error-text");
    totalLabel.getStyleClass().remove("error-text");
    try {
      BigDecimal total =
          context
              .rentalRequestService()
              .estimateTotal(
                  listing == null ? null : listing.getId(),
                  FormValidator.parseDateText(startPicker.getEditor().getText()),
                  FormValidator.parseDateText(endPicker.getEditor().getText()))
              .orElse(null);
      totalLabel.setText(total == null ? "—" : Labels.money(total));
    } catch (DateTimeParseException error) {
      totalLabel.setText("Проверьте даты аренды");
    } catch (BusinessRuleException error) {
      totalLabel.setText(error.getMessage());
      totalLabel.getStyleClass().add("error-text");
    }
    if (wasError != totalLabel.getStyleClass().contains("error-text")
        && totalLabel.getScene() != null) {
      Window window = totalLabel.getScene().getWindow();
      if (window != null) {
        Platform.runLater(window::sizeToScene);
      }
    }
  }
}
