package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Choices;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.FormLoader;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.Tables;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.service.ReviewDetails;
import rentalmarketplace.service.ReviewQuery;
import rentalmarketplace.service.ReviewService;
import rentalmarketplace.service.ReviewSort;

public class ReviewsController implements Refreshable, Exportable {
  private final AppContext context;
  private final ReviewService service;
  private final ObservableList<ReviewDetails> items = FXCollections.observableArrayList();
  private ReviewQuery query = ReviewQuery.all();

  @FXML private TextField searchField;
  @FXML private ComboBox<Integer> ratingFilterBox;
  @FXML private ComboBox<ReviewSort> sortBox;
  @FXML private CheckBox descendingBox;
  @FXML private TableView<ReviewDetails> table;
  @FXML private TableColumn<ReviewDetails, Integer> idColumn;
  @FXML private TableColumn<ReviewDetails, String> listingColumn;
  @FXML private TableColumn<ReviewDetails, String> renterColumn;
  @FXML private TableColumn<ReviewDetails, Integer> ratingColumn;
  @FXML private TableColumn<ReviewDetails, String> commentColumn;
  @FXML private TableColumn<ReviewDetails, LocalDateTime> createdColumn;
  @FXML private Button editButton;
  @FXML private Button deleteButton;
  @FXML private Label countLabel;
  @FXML private Label summaryLabel;

  public ReviewsController(AppContext context) {
    this.context = context;
    this.service = context.reviewService();
  }

  @FXML
  private void initialize() {
    Tables.column(idColumn, ReviewDetails::reviewId);
    Tables.column(listingColumn, ReviewDetails::listingTitle);
    Tables.column(renterColumn, ReviewDetails::renterName);
    Tables.column(ratingColumn, ReviewDetails::rating);
    Tables.column(commentColumn, review -> review.comment() == null ? "" : review.comment());
    Tables.column(createdColumn, ReviewDetails::createdAt, Labels::dateTime);
    Tables.showSorted(table, items);
    table.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> onSelected());

    Choices.setup(ratingFilterBox, "Любая оценка", List.of(5, 4, 3, 2, 1), String::valueOf);
    Choices.setup(sortBox, "Без сортировки", List.of(ReviewSort.values()), Labels::reviewSort);
    onSelected();
    refresh();
  }

  @Override
  public void refresh() {
    ReviewDetails selected = table.getSelectionModel().getSelectedItem();
    try {
      items.setAll(service.query(query));
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    Tables.reselect(table, items, selected, ReviewDetails::reviewId);
    boolean filtered = !ReviewQuery.all().equals(query);
    table.setPlaceholder(
        new Label(
            filtered
                ? "Ничего не найдено. Измените условия или нажмите «Сбросить»"
                : "Отзывов пока нет. Они появятся для завершенных заявок"));
    countLabel.setText("Отзывов: " + items.size());
    onSelected();
  }

  @Override
  public String exportName() {
    return "Отзывы";
  }

  @Override
  public List<String> headers() {
    return List.of("Номер", "Объект", "Арендатор", "Оценка", "Комментарий", "Создан");
  }

  @Override
  public List<List<Object>> rows() {
    List<List<Object>> rows = new ArrayList<>();
    for (ReviewDetails row : table.getItems()) {
      rows.add(
          List.of(
              row.reviewId(),
              row.listingTitle(),
              row.renterName(),
              row.rating(),
              row.comment() == null ? "" : row.comment(),
              row.createdAt() == null ? "" : row.createdAt()));
    }
    return rows;
  }

  @FXML
  private void onApply() {
    query =
        new ReviewQuery(
            searchField.getText(),
            ratingFilterBox.getValue(),
            sortBox.getValue(),
            descendingBox.isSelected());
    refresh();
  }

  @FXML
  private void onReset() {
    searchField.clear();
    ratingFilterBox.getSelectionModel().selectFirst();
    sortBox.getSelectionModel().selectFirst();
    descendingBox.setSelected(false);
    query = ReviewQuery.all();
    refresh();
  }

  @FXML
  private void onAdd() {
    if (service.getReviewableRequests().isEmpty()) {
      Dialogs.message(
          "Отзыв оставить не на что",
          "Нет завершенных заявок без отзыва. Отзыв можно оставить только на завершенную заявку.");
      return;
    }
    FormLoader.Loaded<ReviewFormController> form =
        FormLoader.<ReviewFormController>load(context, "/fxml/review-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    boolean saved =
        Dialogs.saveForm(
            "Новый отзыв",
            form.node(),
            form.controller()::validate,
            () -> {
              ReviewFormController.Values values = form.controller().values();
              service.create(values.requestId(), values.rating(), values.comment());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onEdit() {
    ReviewDetails selected = table.getSelectionModel().getSelectedItem();
    if (selected == null || !context.access().mayEdit(selected.renterId())) {
      return;
    }
    FormLoader.Loaded<ReviewFormController> form =
        FormLoader.<ReviewFormController>load(context, "/fxml/review-form.fxml").orElse(null);
    if (form == null) {
      return;
    }
    form.controller().prepareForEdit(selected);
    boolean saved =
        Dialogs.saveForm(
            "Отзыв №" + selected.reviewId(),
            form.node(),
            form.controller()::validate,
            () -> {
              ReviewFormController.Values values = form.controller().values();
              service.update(selected.reviewId(), values.rating(), values.comment());
            });
    if (saved) {
      refresh();
    }
  }

  @FXML
  private void onDelete() {
    ReviewDetails row = table.getSelectionModel().getSelectedItem();
    if (row == null || !context.access().mayEdit(row.renterId())) {
      return;
    }
    boolean confirmed =
        Dialogs.confirm(
            "Удалить отзыв №" + row.reviewId() + "?",
            "Отзыв на «"
                + row.listingTitle()
                + "» от "
                + row.renterName()
                + " будет удален без возможности восстановления.");
    if (!confirmed) {
      return;
    }
    try {
      service.delete(row.reviewId());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    refresh();
  }

  private void onSelected() {
    ReviewDetails selected = table.getSelectionModel().getSelectedItem();
    boolean editable = selected != null && context.access().mayEdit(selected.renterId());
    editButton.setDisable(!editable);
    deleteButton.setDisable(!editable);
    if (selected == null) {
      summaryLabel.setText("Выберите отзыв, чтобы увидеть средний рейтинг объекта");
      return;
    }
    try {
      BigDecimal average = service.averageRatingForListing(selected.listingId()).orElse(null);
      summaryLabel.setText(
          "Объект «"
              + selected.listingTitle()
              + "»: средний рейтинг "
              + (average == null ? "нет данных" : average.toPlainString())
              + ", заявка №"
              + selected.requestId()
              + ", арендатор "
              + selected.renterName());
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
    }
  }
}
