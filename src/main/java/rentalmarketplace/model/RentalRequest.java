package rentalmarketplace.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalRequest implements Displayable {
  private Integer id;
  private Integer listingId;
  private Integer renterId;
  private LocalDate startDate;
  private LocalDate endDate;
  private RentalRequestStatus status;
  private BigDecimal totalPrice;
  private LocalDateTime createdAt;

  /** Используется при создании новой заявки — id и createdAt назначит база данных. */
  public RentalRequest(
      Integer listingId,
      Integer renterId,
      LocalDate startDate,
      LocalDate endDate,
      RentalRequestStatus status,
      BigDecimal totalPrice) {
    this(null, listingId, renterId, startDate, endDate, status, totalPrice, null);
  }

  /** Используется при загрузке уже существующей заявки (из БД или in-memory хранилища). */
  public RentalRequest(
      Integer id,
      Integer listingId,
      Integer renterId,
      LocalDate startDate,
      LocalDate endDate,
      RentalRequestStatus status,
      BigDecimal totalPrice,
      LocalDateTime createdAt) {
    this.id = id;
    this.listingId = listingId;
    this.renterId = renterId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
    this.totalPrice = totalPrice;
    this.createdAt = createdAt;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getListingId() {
    return listingId;
  }

  public void setListingId(Integer listingId) {
    this.listingId = listingId;
  }

  public Integer getRenterId() {
    return renterId;
  }

  public void setRenterId(Integer renterId) {
    this.renterId = renterId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public RentalRequestStatus getStatus() {
    return status;
  }

  public void setStatus(RentalRequestStatus status) {
    this.status = status;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(BigDecimal totalPrice) {
    this.totalPrice = totalPrice;
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
        "%-4s %-6s %-6s %-12s %-12s %-10s %-10s %-20s",
        id == null ? "-" : id,
        listingId == null ? "-" : listingId,
        renterId == null ? "-" : renterId,
        startDate == null ? "-" : startDate,
        endDate == null ? "-" : endDate,
        status == null ? "-" : status,
        totalPrice == null ? "-" : totalPrice,
        createdAt == null ? "-" : createdAt);
  }

  @Override
  public String toString() {
    return toTableRow();
  }
}
