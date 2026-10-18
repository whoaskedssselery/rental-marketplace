package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.Tables;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.service.StatisticsReport;
import rentalmarketplace.service.StatisticsService;

public class StatisticsController implements Refreshable, Exportable {
  private record Figure(String name, Object value, String text) {
    static Figure of(String name, Object value) {
      return new Figure(name, value, String.valueOf(value));
    }

    static Figure money(String name, BigDecimal amount) {
      return new Figure(name, amount, Labels.money(amount));
    }
  }

  private record CountRow(String name, long count) {}

  private final StatisticsService service;
  private final List<Figure> allFigures = new ArrayList<>();

  @FXML private VBox requestsBox;
  @FXML private VBox moneyBox;
  @FXML private VBox catalogBox;
  @FXML private TableView<CountRow> statusTable;
  @FXML private TableColumn<CountRow, String> statusNameColumn;
  @FXML private TableColumn<CountRow, Long> statusCountColumn;
  @FXML private TableView<CountRow> categoryTable;
  @FXML private TableColumn<CountRow, String> categoryNameColumn;
  @FXML private TableColumn<CountRow, Long> categoryCountColumn;
  @FXML private DatePicker fromPicker;
  @FXML private DatePicker toPicker;
  @FXML private Label periodResult;

  public StatisticsController(AppContext context) {
    this.service = context.statisticsService();
  }

  @FXML
  private void initialize() {
    Tables.column(statusNameColumn, CountRow::name);
    Tables.column(statusCountColumn, CountRow::count);
    Tables.column(categoryNameColumn, CountRow::name);
    Tables.column(categoryCountColumn, CountRow::count);
    refresh();
  }

  @Override
  public void refresh() {
    StatisticsReport report;
    try {
      report = service.collect();
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    allFigures.clear();
    show(
        requestsBox,
        List.of(
            Figure.of("Всего заявок", report.totalRequests()),
            Figure.of("Активные (новые, подтвержденные, в аренде)", report.activeRequests()),
            Figure.of("Завершенные", report.completedRequests()),
            Figure.of("Отмененные и отклоненные", report.cancelledOrRejectedRequests()),
            Figure.of("Доля отмененных и отклоненных", report.cancelledSharePercent() + " %")));
    show(
        moneyBox,
        List.of(
            Figure.money("Средняя сумма", report.averageTotalPrice()),
            Figure.money("Минимальная сумма", report.minTotalPrice()),
            Figure.money("Максимальная сумма", report.maxTotalPrice())));
    show(
        catalogBox,
        List.of(
            Figure.of("Пользователей", report.totalUsers()),
            Figure.of("Объектов аренды", report.totalListings()),
            Figure.of("Отзывов", report.totalReviews()),
            Figure.of(
                "Средний рейтинг",
                report.averageRating() == null ? "нет данных" : report.averageRating())));

    List<CountRow> byStatus = new ArrayList<>();
    report
        .requestsByStatus()
        .forEach(
            (status, count) -> {
              byStatus.add(new CountRow(Labels.status(status), count));
              allFigures.add(Figure.of("Заявок: " + Labels.status(status), count));
            });
    statusTable.setItems(FXCollections.observableArrayList(byStatus));

    List<CountRow> byCategory = new ArrayList<>();
    report
        .listingsByCategory()
        .forEach(
            (category, count) -> {
              byCategory.add(new CountRow(category, count));
              allFigures.add(Figure.of("Объектов в категории " + category, count));
            });
    categoryTable.setItems(FXCollections.observableArrayList(byCategory));
  }

  @Override
  public String exportName() {
    return "Статистика";
  }

  @Override
  public List<String> headers() {
    return List.of("Показатель", "Значение");
  }

  @Override
  public List<List<Object>> rows() {
    List<List<Object>> rows = new ArrayList<>();
    for (Figure figure : allFigures) {
      rows.add(List.of(figure.name(), figure.value()));
    }
    return rows;
  }

  @FXML
  private void onCountPeriod() {
    try {
      long count = service.countRequestsInPeriod(fromPicker.getValue(), toPicker.getValue());
      periodResult.setText("Заявок за период: " + count);
    } catch (RuntimeException e) {
      periodResult.setText("");
      Dialogs.error(UserMessages.of(e));
    }
  }

  private void show(VBox box, List<Figure> figures) {
    box.getChildren().clear();
    for (Figure figure : figures) {
      Label name = new Label(figure.name());
      name.getStyleClass().add("stat-name");
      Label value = new Label(figure.text());
      value.getStyleClass().add("stat-figure");
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      HBox row = new HBox(name, spacer, value);
      row.setAlignment(Pos.CENTER_LEFT);
      row.getStyleClass().add("stat-row");
      box.getChildren().add(row);
    }
    allFigures.addAll(figures);
  }
}
