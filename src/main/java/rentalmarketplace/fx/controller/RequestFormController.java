package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
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
  @FXML private Label hintLabel;

  public RequestFormController(AppContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
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
    renterBox.setItems(FXCollections.observableArrayList(context.userService().getAllUsers()));
    listingBox.valueProperty().addListener((o, was, now) -> updateTotal());
    startPicker.valueProperty().addListener((o, was, now) -> updateTotal());
    endPicker.valueProperty().addListener((o, was, now) -> updateTotal());
    updateTotal();
  }

  public void prepareForCreate() {
    List<Listing> available =
        context.listingService().getAllListings().stream().filter(Listing::isAvailable).toList();
    listingBox.setItems(FXCollections.observableArrayList(available));
    hintLabel.setText("Объект и арендатора выбирайте из списков. Сумма считается автоматически.");
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
    if (context.rentalRequestService().canChangeDates(details.status())) {
      hintLabel.setText("Можно изменить только даты: объект и арендатор у заявки не меняются.");
    } else {
      startPicker.setDisable(true);
      endPicker.setDisable(true);
      hintLabel.setText(
          "Даты можно менять только у новой заявки. Статус меняется отдельной кнопкой.");
    }
  }

  public ValidationResult validate() {
    FormValidator validator = new FormValidator();
    validator.selected("Объект", listingBox.getValue());
    validator.selected("Арендатор", renterBox.getValue());
    LocalDate start = validator.requiredDate("Дата начала", startPicker.getValue());
    LocalDate end = validator.requiredDate("Дата окончания", endPicker.getValue());
    validator.endAfterStart("Дата окончания", start, end);
    return validator.result();
  }

  public Values values() {
    return new Values(
        listingBox.getValue().getId(),
        renterBox.getValue().getId(),
        startPicker.getValue(),
        endPicker.getValue());
  }

  private void updateTotal() {
    Listing listing = listingBox.getValue();
    BigDecimal total =
        context
            .rentalRequestService()
            .estimateTotal(
                listing == null ? null : listing.getId(),
                startPicker.getValue(),
                endPicker.getValue())
            .orElse(null);
    totalLabel.setText(
        total == null ? "Итого: выберите объект и даты" : "Итого: " + Labels.money(total));
  }
}
