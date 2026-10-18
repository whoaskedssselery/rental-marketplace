package rentalmarketplace.fx.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import rentalmarketplace.fx.AppContext;
import rentalmarketplace.fx.support.Dialogs;
import rentalmarketplace.fx.support.Exportable;
import rentalmarketplace.fx.support.Labels;
import rentalmarketplace.fx.support.Refreshable;
import rentalmarketplace.fx.support.UserMessages;
import rentalmarketplace.service.StatisticsReport;
import rentalmarketplace.service.StatisticsService;

public class StatisticsController implements Refreshable, Exportable {
  private record CountRow(String name, long count) {}

  private final StatisticsService service;
  private StatisticsReport report;

  @FXML private Label usersValue;
  @FXML private Label listingsValue;
  @FXML private Label requestsValue;
  @FXML private Label reviewsValue;
  @FXML private Label activeValue;
  @FXML private Label completedValue;
  @FXML private Label cancelledValue;
  @FXML private Label shareValue;
  @FXML private Label averageValue;
  @FXML private Label minValue;
  @FXML private Label maxValue;
  @FXML private Label ratingValue;
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
    statusNameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().name()));
    statusCountColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().count()));
    categoryNameColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().name()));
    categoryCountColumn.setCellValueFactory(
        row -> new ReadOnlyObjectWrapper<>(row.getValue().count()));
    refresh();
  }

  @Override
  public void refresh() {
    try {
      report = service.collect();
    } catch (RuntimeException e) {
      Dialogs.error(UserMessages.of(e));
      return;
    }
    usersValue.setText(String.valueOf(report.totalUsers()));
    listingsValue.setText(String.valueOf(report.totalListings()));
    requestsValue.setText(String.valueOf(report.totalRequests()));
    reviewsValue.setText(String.valueOf(report.totalReviews()));
    activeValue.setText(String.valueOf(report.activeRequests()));
    completedValue.setText(String.valueOf(report.completedRequests()));
    cancelledValue.setText(String.valueOf(report.cancelledOrRejectedRequests()));
    shareValue.setText(report.cancelledSharePercent().toPlainString() + " %");
    averageValue.setText(Labels.money(report.averageTotalPrice()));
    minValue.setText(Labels.money(report.minTotalPrice()));
    maxValue.setText(Labels.money(report.maxTotalPrice()));
    ratingValue.setText(ratingText(report.averageRating()));

    List<CountRow> byStatus = new ArrayList<>();
    report
        .requestsByStatus()
        .forEach((status, count) -> byStatus.add(new CountRow(Labels.status(status), count)));
    statusTable.setItems(FXCollections.observableArrayList(byStatus));
    List<CountRow> byCategory = new ArrayList<>();
    report
        .listingsByCategory()
        .forEach((category, count) -> byCategory.add(new CountRow(category, count)));
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
    if (report == null) {
      return rows;
    }
    rows.add(List.of("Пользователей", report.totalUsers()));
    rows.add(List.of("Объектов аренды", report.totalListings()));
    rows.add(List.of("Заявок всего", report.totalRequests()));
    rows.add(List.of("Отзывов", report.totalReviews()));
    rows.add(List.of("Активных заявок", report.activeRequests()));
    rows.add(List.of("Завершенных заявок", report.completedRequests()));
    rows.add(List.of("Отмененных и отклоненных заявок", report.cancelledOrRejectedRequests()));
    rows.add(List.of("Доля отмененных и отклоненных, %", report.cancelledSharePercent()));
    rows.add(List.of("Средняя сумма заявки", report.averageTotalPrice()));
    rows.add(List.of("Минимальная сумма заявки", report.minTotalPrice()));
    rows.add(List.of("Максимальная сумма заявки", report.maxTotalPrice()));
    rows.add(List.of("Средний рейтинг отзывов", ratingText(report.averageRating())));
    report
        .requestsByStatus()
        .forEach((status, count) -> rows.add(List.of("Заявок: " + Labels.status(status), count)));
    report
        .listingsByCategory()
        .forEach((category, count) -> rows.add(List.of("Объектов в категории " + category, count)));
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

  private String ratingText(BigDecimal rating) {
    return rating == null ? "нет данных" : rating.toPlainString();
  }
}
