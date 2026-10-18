package rentalmarketplace.service;

public record ReviewQuery(String text, Integer rating, ReviewSort sort, boolean descending) {

  public static ReviewQuery all() {
    return new ReviewQuery(null, null, null, false);
  }
}
