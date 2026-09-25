package rentalmarketplace.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Listing implements Displayable {
  private static final String ROW_FORMAT = "%-4s %-8s %-30s %-12s %-15s %-9s";
  public static final String TABLE_HEADER =
      String.format(ROW_FORMAT, "id", "Владелец", "Название", "Цена/день", "Категория", "Доступен");

  private Integer id;
  private Integer ownerId;
  private String title;
  private String description;
  private BigDecimal pricePerDay;
  private String category;
  private boolean available;
  private LocalDateTime createdAt;

  public Listing(
      Integer ownerId,
      String title,
      String description,
      BigDecimal pricePerDay,
      String category,
      boolean available) {
    this(null, ownerId, title, description, pricePerDay, category, available, null);
  }

  public Listing(
      Integer id,
      Integer ownerId,
      String title,
      String description,
      BigDecimal pricePerDay,
      String category,
      boolean available,
      LocalDateTime createdAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
    this.pricePerDay = pricePerDay;
    this.category = category;
    this.available = available;
    this.createdAt = createdAt;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Integer ownerId) {
    this.ownerId = ownerId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getPricePerDay() {
    return pricePerDay;
  }

  public void setPricePerDay(BigDecimal pricePerDay) {
    this.pricePerDay = pricePerDay;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toTableRow() {
    return String.format(
        ROW_FORMAT,
        id == null ? "-" : id,
        ownerId == null ? "-" : ownerId,
        title,
        pricePerDay,
        category,
        available ? "да" : "нет");
  }

  @Override
  public String toString() {
    return toTableRow();
  }
}
