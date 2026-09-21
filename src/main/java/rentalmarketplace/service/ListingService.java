package rentalmarketplace.service;

import java.math.BigDecimal;
import java.util.List;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.exception.EntityNotFoundException;
import rentalmarketplace.model.Listing;
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
    validateTitle(title);
    validateCategory(category);
    validatePrice(pricePerDay);
    userService.getUserById(ownerId);
    Listing listing = new Listing(ownerId, title.trim(), description, pricePerDay, category, true);
    return listingRepository.save(listing);
  }

  public Listing getListingById(Integer id) {
    return listingRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Объект аренды с id=" + id + " не найден"));
  }

  public List<Listing> getAllListings() {
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
    validateTitle(title);
    validateCategory(category);
    validatePrice(pricePerDay);
    userService.getUserById(ownerId);
    existing.setOwnerId(ownerId);
    existing.setTitle(title.trim());
    existing.setDescription(description);
    existing.setPricePerDay(pricePerDay);
    existing.setCategory(category);
    existing.setAvailable(available);
    return listingRepository.update(existing);
  }

  public void deleteListing(Integer id) {
    getListingById(id);
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
    if (category != null && category.length() > CATEGORY_MAX_LENGTH) {
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
