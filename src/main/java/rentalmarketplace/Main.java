package rentalmarketplace;

import rentalmarketplace.repository.ListingRepository;
import rentalmarketplace.repository.ListingRepositoryJdbc;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.repository.UserRepositoryJdbc;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.UserService;
import rentalmarketplace.ui.ConsoleMenu;

public class Main {
  public static void main(String[] args) {
    UserRepository userRepository = new UserRepositoryJdbc();
    UserService userService = new UserService(userRepository);
    ListingRepository listingRepository = new ListingRepositoryJdbc();
    ListingService listingService = new ListingService(listingRepository, userService);
    new ConsoleMenu(userService, listingService).run();
  }
}
