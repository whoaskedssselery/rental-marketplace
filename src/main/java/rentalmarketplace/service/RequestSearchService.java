package rentalmarketplace.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.User;

public class RequestSearchService {
  private final RentalRequestService requestService;
  private final ListingService listingService;
  private final UserService userService;

  public RequestSearchService(
      RentalRequestService requestService, ListingService listingService, UserService userService) {
    this.requestService = requestService;
    this.listingService = listingService;
    this.userService = userService;
  }

  public List<RequestDetails> query(RequestQuery query) {
    Map<Integer, Listing> listingsById =
        listingService.getAllListings().stream()
            .collect(Collectors.toMap(Listing::getId, Function.identity()));
    Map<Integer, User> usersById =
        userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    String text = normalize(query.text());
    Comparator<RequestDetails> order = comparatorFor(query);
    return requestService.getAll().stream()
        .map(request -> toDetails(request, listingsById, usersById))
        .filter(details -> matchesQuery(details, listingsById, text, query))
        .sorted(query.descending() ? order.reversed() : order)
        .toList();
  }

  private RequestDetails toDetails(
      RentalRequest request, Map<Integer, Listing> listingsById, Map<Integer, User> usersById) {
    Listing listing = listingsById.get(request.getListingId());
    User renter = usersById.get(request.getRenterId());
    User owner = usersById.get(listing.getOwnerId());
    return new RequestDetails(
        request.getId(),
        listing.getId(),
        listing.getTitle(),
        renter.getId(),
        renter.getFullName(),
        renter.getEmail(),
        owner.getId(),
        owner.getFullName(),
        request.getStartDate(),
        request.getEndDate(),
        request.getStatus(),
        request.getTotalPrice(),
        request.getCreatedAt());
  }

  private boolean matchesQuery(
      RequestDetails details, Map<Integer, Listing> listingsById, String text, RequestQuery query) {
    return (query.status() == null || details.status() == query.status())
        && (query.ownerId() == null || details.ownerId().equals(query.ownerId()))
        && (query.listingId() == null || details.listingId().equals(query.listingId()))
        && (query.renterId() == null || details.renterId().equals(query.renterId()))
        && (query.periodFrom() == null || !details.endDate().isBefore(query.periodFrom()))
        && (query.periodTo() == null || !details.startDate().isAfter(query.periodTo()))
        && (text.isEmpty() || matchesText(details, listingsById.get(details.listingId()), text));
  }

  private boolean matchesText(RequestDetails details, Listing listing, String text) {
    return containsIgnoreCase(details.listingTitle(), text)
        || containsIgnoreCase(listing.getDescription(), text)
        || containsIgnoreCase(details.renterName(), text)
        || containsIgnoreCase(details.renterEmail(), text)
        || containsIgnoreCase(details.ownerName(), text);
  }

  private Comparator<RequestDetails> comparatorFor(RequestQuery query) {
    Comparator<RequestDetails> byId = Comparator.comparing(RequestDetails::requestId);
    if (query.sort() == null) {
      return byId;
    }
    Comparator<RequestDetails> main =
        switch (query.sort()) {
          case CREATED_AT ->
              Comparator.comparing(
                  RequestDetails::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
          case TOTAL_PRICE -> Comparator.comparing(RequestDetails::totalPrice);
          case START_DATE -> Comparator.comparing(RequestDetails::startDate);
          case LISTING_TITLE ->
              Comparator.comparing(RequestDetails::listingTitle, String.CASE_INSENSITIVE_ORDER);
        };
    return main.thenComparing(byId);
  }

  private String normalize(String text) {
    return text == null ? "" : text.trim().toLowerCase();
  }

  private boolean containsIgnoreCase(String value, String lowerCaseText) {
    return value != null && value.toLowerCase().contains(lowerCaseText);
  }
}
