package rentalmarketplace;

import rentalmarketplace.repository.ListingRepository;
import rentalmarketplace.repository.RentalRequestRepository;
import rentalmarketplace.repository.SchemaRepository;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.ReportService;
import rentalmarketplace.service.UserService;
import rentalmarketplace.ui.ConsoleInput;
import rentalmarketplace.ui.ConsoleMenu;
import rentalmarketplace.ui.ListingMenu;
import rentalmarketplace.ui.RentalRequestMenu;
import rentalmarketplace.ui.UserMenu;

public class Main {
  public static void main(String[] args) {
    UserService userService = new UserService(new UserRepository());
    ListingService listingService = new ListingService(new ListingRepository(), userService);
    RentalRequestService rentalRequestService =
        new RentalRequestService(new RentalRequestRepository(), listingService, userService);
    ReportService reportService =
        new ReportService(
            userService, listingService, rentalRequestService, new SchemaRepository());

    ConsoleInput input = new ConsoleInput();
    ConsoleMenu mainMenu =
        new ConsoleMenu(
            input,
            new UserMenu(userService, input),
            new ListingMenu(listingService, userService, input),
            new RentalRequestMenu(rentalRequestService, listingService, userService, input),
            reportService);
    mainMenu.run();
  }
}
