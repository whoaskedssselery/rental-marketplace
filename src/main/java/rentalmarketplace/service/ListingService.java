package rentalmarketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.User;
import rentalmarketplace.repository.ListingRepository;

public class ListingService {
  private static final int TITLE_MAX_LENGTH = 200;
  private static final int CATEGORY_MAX_LENGTH = 50;

  private final ListingRepository listingRepository;
  private final UserService userService;

  public ListingService(ListingRepository listingRepository, UserService userService) {
    this.listingRepository = listingRepository;
    this.userService = userService;
  }

  public Listing createListing(
      Integer ownerId, String title, String description, BigDecimal pricePerDay, String category) {
    return createListing(ownerId, title, description, pricePerDay, category, true);
  }

  public Listing createListing(
      Integer ownerId,
      String title,
      String description,
      BigDecimal pricePerDay,
      String category,
      boolean available) {
    userService.access().requireOwner(ownerId);
    validateTitle(title);
    validateCategory(category);
    validatePrice(pricePerDay);
    userService.getUserById(ownerId);
    Listing listing =
        new Listing(ownerId, title.trim(), description, pricePerDay, category.trim(), available);
    return listingRepository.save(listing);
  }

  public Listing getListingById(Integer id) {
    userService.access().currentUser();
    return listingRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Объект аренды с id=" + id + " не найден"));
  }

  public List<Listing> getAllListings() {
    userService.access().currentUser();
    return listingRepository.findAll();
  }

  public Listing updateListing(
      Integer id,
      Integer ownerId,
      String title,
      String description,
      BigDecimal pricePerDay,
      String category,
      boolean available) {
    Listing existing = getListingById(id);
    userService.access().requireOwner(existing.getOwnerId());
    userService.access().requireOwner(ownerId);
    validateTitle(title);
    validateCategory(category);
    validatePrice(pricePerDay);
    userService.getUserById(ownerId);
    existing.setOwnerId(ownerId);
    existing.setTitle(title.trim());
    existing.setDescription(description);
    existing.setPricePerDay(pricePerDay);
    existing.setCategory(category.trim());
    existing.setAvailable(available);
    return listingRepository.update(existing);
  }

  public List<ListingDetails> getDetails() {
    Map<Integer, User> usersById =
        userService.getAllUsers().stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    return getAllListings().stream()
        .map(
            listing ->
                new ListingDetails(
                    listing.getId(),
                    listing.getOwnerId(),
                    usersById.get(listing.getOwnerId()).getFullName(),
                    listing.getTitle(),
                    listing.getDescription(),
                    listing.getPricePerDay(),
                    listing.getCategory(),
                    listing.isAvailable(),
                    listing.getCreatedAt()))
        .toList();
  }

  public List<ListingDetails> getDetailsByOwner(Integer ownerId) {
    return getDetails().stream().filter(details -> details.ownerId().equals(ownerId)).toList();
  }

  public List<String> getCategories() {
    return getAllListings().stream()
        .map(Listing::getCategory)
        .distinct()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  public void deleteListing(Integer id) {
    userService.access().requireOwner(getListingById(id).getOwnerId());
    listingRepository.deleteById(id);
  }

  private void validateTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new BusinessRuleException("Название объекта не может быть пустым");
    }
    if (title.trim().length() > TITLE_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Название объекта не может быть длиннее " + TITLE_MAX_LENGTH + " символов");
    }
  }

  private void validateCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new BusinessRuleException("Категория не может быть пустой");
    }
    if (category.trim().length() > CATEGORY_MAX_LENGTH) {
      throw new BusinessRuleException(
          "Категория не может быть длиннее " + CATEGORY_MAX_LENGTH + " символов");
    }
  }

  private void validatePrice(BigDecimal pricePerDay) {
    if (pricePerDay == null || pricePerDay.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException("Цена за день должна быть положительной");
    }
  }
}
