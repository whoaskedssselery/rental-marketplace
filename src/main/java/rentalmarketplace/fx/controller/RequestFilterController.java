package rentalmarketplace.fx.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Choices;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.RequestQuery;
import rentalmarketplace.service.RequestSort;

public class RequestFilterController {
  private final AppContext context;
  private Consumer<RequestQuery> onApply = query -> {};

  @FXML private TextField searchField;
  @FXML private ComboBox<RentalRequestStatus> statusBox;
  @FXML private ComboBox<User> ownerBox;
  @FXML private DatePicker fromPicker;
  @FXML private DatePicker toPicker;
  @FXML private ComboBox<RequestSort> sortBox;
  @FXML private CheckBox descendingBox;
  @FXML private Label filterError;

  public RequestFilterController(AppContext context) {
    this.context = context;
  }

  public void setOnApply(Consumer<RequestQuery> onApply) {
    this.onApply = onApply;
  }

  @FXML
  private void initialize() {
    Choices.setup(statusBox, "Все статусы", List.of(RentalRequestStatus.values()), Labels::status);
    Choices.setup(sortBox, "Без сортировки", List.of(RequestSort.values()), Labels::sort);
    refreshOwners(RequestQuery.all());
  }

  public RequestQuery refreshOwners(RequestQuery appliedQuery) {
    User selected = ownerBox.getValue();
    List<User> users = context.userService().getAllUsers();
    Set<Integer> listingOwners =
        context.listingService().getAllListings().stream()
            .map(Listing::getOwnerId)
            .collect(Collectors.toSet());
    List<User> owners =
        users.stream()
            .filter(
                user ->
                    user.getRole() == UserRole.OWNER
                        || listingOwners.contains(user.getId())
                        || (selected != null && user.getId().equals(selected.getId()))
                        || user.getId().equals(appliedQuery.ownerId()))
            .toList();
    Choices.setup(ownerBox, "Все владельцы", owners, User::getFullName);
    if (selected != null) {
      owners.stream()
          .filter(user -> user.getId().equals(selected.getId()))
          .findFirst()
          .ifPresent(ownerBox::setValue);
    }
    if (appliedQuery.ownerId() == null
        || users.stream().anyMatch(user -> user.getId().equals(appliedQuery.ownerId()))) {
      return appliedQuery;
    }
    return new RequestQuery(
        appliedQuery.text(),
        appliedQuery.status(),
        null,
        appliedQuery.listingId(),
        appliedQuery.renterId(),
        appliedQuery.periodFrom(),
        appliedQuery.periodTo(),
        appliedQuery.sort(),
        appliedQuery.descending());
  }

  @FXML
  private void onApply() {
    LocalDate from = fromPicker.getValue();
    LocalDate to = toPicker.getValue();
    if (from != null && to != null && to.isBefore(from)) {
      filterError.setText("Конец периода должен быть не раньше начала.");
      filterError.setVisible(true);
      filterError.setManaged(true);
      return;
    }
    filterError.setVisible(false);
    filterError.setManaged(false);
    User owner = ownerBox.getValue();
    onApply.accept(
        new RequestQuery(
            searchField.getText(),
            statusBox.getValue(),
            owner == null ? null : owner.getId(),
            null,
            null,
            from,
            to,
            sortBox.getValue(),
            descendingBox.isSelected()));
  }

  @FXML
  private void onReset() {
    filterError.setVisible(false);
    filterError.setManaged(false);
    searchField.clear();
    statusBox.getSelectionModel().selectFirst();
    ownerBox.getSelectionModel().selectFirst();
    sortBox.getSelectionModel().selectFirst();
    fromPicker.setValue(null);
    toPicker.setValue(null);
    descendingBox.setSelected(false);
    onApply.accept(RequestQuery.all());
  }
}
