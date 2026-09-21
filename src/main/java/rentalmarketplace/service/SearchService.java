package rentalmarketplace.service;

import java.util.List;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.User;

public class SearchService {
  private final RentalRequestService rentalRequestService;
  private final ListingService listingService;
  private final UserService userService;

  public SearchService(
      RentalRequestService rentalRequestService,
      ListingService listingService,
      UserService userService) {
    this.rentalRequestService = rentalRequestService;
    this.listingService = listingService;
    this.userService = userService;
  }

  public List<RentalRequest> searchByListing(String query) {
    String q = query == null ? "" : query.trim().toLowerCase();
    return rentalRequestService.getAll().stream()
        .filter(request -> matchesListing(request, q))
        .toList();
  }

  public List<RentalRequest> searchByRenter(String query) {
    String q = query == null ? "" : query.trim().toLowerCase();
    return rentalRequestService.getAll().stream()
        .filter(request -> matchesRenter(request, q))
        .toList();
  }

  private boolean matchesListing(RentalRequest request, String query) {
    try {
      Listing listing = listingService.getListingById(request.getListingId());
      String title = listing.getTitle() == null ? "" : listing.getTitle().toLowerCase();
      String description =
          listing.getDescription() == null ? "" : listing.getDescription().toLowerCase();
      return title.contains(query) || description.contains(query);
    } catch (EntityNotFoundException e) {
      return false;
    }
  }

  private boolean matchesRenter(RentalRequest request, String query) {
    try {
      User renter = userService.getUserById(request.getRenterId());
      String fullName = renter.getFullName() == null ? "" : renter.getFullName().toLowerCase();
      String email = renter.getEmail() == null ? "" : renter.getEmail().toLowerCase();
      return fullName.contains(query) || email.contains(query);
    } catch (EntityNotFoundException e) {
      return false;
    }
  }
}
