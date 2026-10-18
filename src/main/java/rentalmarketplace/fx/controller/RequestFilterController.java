package rentalmarketplace.fx.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Choices;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Labels;
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

  public RequestFilterController(AppContext context) {
    this.context = context;
  }

  public void setOnApply(Consumer<RequestQuery> onApply) {
    this.onApply = onApply;
  }

  @FXML
  private void initialize() {
    Choices.setup(statusBox, "Все статусы", List.of(RentalRequestStatus.values()), Labels::status);
    List<User> owners =
        context.userService().getAllUsers().stream()
            .filter(user -> user.getRole() == UserRole.OWNER)
            .toList();
    Choices.setup(ownerBox, "Все владельцы", owners, User::getFullName);
    Choices.setup(sortBox, "Без сортировки", List.of(RequestSort.values()), Labels::sort);
  }

  @FXML
  private void onApply() {
    LocalDate from = fromPicker.getValue();
    LocalDate to = toPicker.getValue();
    if (from != null && to != null && to.isBefore(from)) {
      Dialogs.error("Период: конец периода не может быть раньше его начала.");
      return;
    }
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
