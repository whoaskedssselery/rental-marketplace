package rentalmarketplace;

import rentalmarketplace.repository.ListingRepository;
import rentalmarketplace.repository.ListingRepositoryJdbc;
import rentalmarketplace.repository.RentalRequestRepository;
import rentalmarketplace.repository.RentalRequestRepositoryJdbc;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.repository.UserRepositoryJdbc;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.SearchService;
import rentalmarketplace.service.UserService;
import rentalmarketplace.ui.ConsoleMenu;

public class Main {
  public static void main(String[] args) {
    UserRepository userRepository = new UserRepositoryJdbc();
    UserService userService = new UserService(userRepository);

    ListingRepository listingRepository = new ListingRepositoryJdbc();
    ListingService listingService = new ListingService(listingRepository, userService);

    RentalRequestRepository rentalRequestRepository = new RentalRequestRepositoryJdbc();
    RentalRequestService rentalRequestService =
        new RentalRequestService(rentalRequestRepository, listingService, userService);

    SearchService searchService =
        new SearchService(rentalRequestService, listingService, userService);

    new ConsoleMenu(userService, listingService, rentalRequestService, searchService).run();
  }
}
