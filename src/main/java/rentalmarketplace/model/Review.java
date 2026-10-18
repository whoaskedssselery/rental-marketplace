package rentalmarketplace.model;

import java.time.LocalDateTime;

public class Review {
  private Integer id;
  private Integer requestId;
  private int rating;
  private String comment;
  private LocalDateTime createdAt;

  public Review(Integer requestId, int rating, String comment) {
    this(null, requestId, rating, comment, null);
  }

  public Review(
      Integer id, Integer requestId, int rating, String comment, LocalDateTime createdAt) {
    this.id = id;
    this.requestId = requestId;
    this.rating = rating;
    this.comment = comment;
    this.createdAt = createdAt;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getRequestId() {
    return requestId;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
