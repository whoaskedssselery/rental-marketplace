package rentalmarketplace;

import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.repository.UserRepositoryJdbc;
import rentalmarketplace.service.UserService;
import rentalmarketplace.ui.ConsoleMenu;

public class Main {
  public static void main(String[] args) {
    UserRepository userRepository = new UserRepositoryJdbc();
    UserService userService = new UserService(userRepository);
    new ConsoleMenu(userService).run();
  }
}
