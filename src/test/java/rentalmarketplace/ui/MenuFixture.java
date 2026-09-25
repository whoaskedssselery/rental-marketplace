package rentalmarketplace.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import rentalmarketplace.model.Listing;
import rentalmarketplace.model.RentalRequest;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.repository.FakeListingRepository;
import rentalmarketplace.repository.FakeRentalRequestRepository;
import rentalmarketplace.repository.FakeUserRepository;
import rentalmarketplace.repository.SchemaRepository;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.ReportService;
import rentalmarketplace.service.UserService;

final class MenuFixture {
  final UserService users = new UserService(new FakeUserRepository());
  final ListingService listings = new ListingService(new FakeListingRepository(), users);
  final RentalRequestService requests =
      new RentalRequestService(new FakeRentalRequestRepository(), listings, users);
  final ReportService reports = new ReportService(users, listings, requests, schema());

  SchemaRepository schema() {
    return new SchemaRepository() {
      @Override
      public List<String> findAllTableNames() {
        return List.of("users", "listings", "rental_requests");
      }
    };
  }

  ConsoleInput input(String... lines) {
    byte[] bytes = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);
    return new ConsoleInput(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
  }

  String capture(Runnable action) {
    PrintStream original = System.out;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    try {
      action.run();
    } finally {
      System.setOut(original);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  User owner() {
    return users.createUser("Владелец", "owner@example.com", null, UserRole.OWNER);
  }

  User renter() {
    return users.createUser("Иван Петров", "ivan@example.com", null, UserRole.RENTER);
  }

  Listing listing(User owner) {
    return listings.createListing(
        owner.getId(), "Дрель", "мощная", new BigDecimal("100.00"), "Инструменты");
  }

  RentalRequest request(Listing listing, User renter, int startDay, int endDay) {
    return requests.create(
        listing.getId(),
        renter.getId(),
        LocalDate.of(2026, 10, startDay),
        LocalDate.of(2026, 10, endDay));
  }
}
