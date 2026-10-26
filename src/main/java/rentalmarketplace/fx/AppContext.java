package rentalmarketplace.fx;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import rentalmarketplace.fx.support.AppFonts;
import rentalmarketplace.repository.ListingRepository;
import rentalmarketplace.repository.RentalRequestRepository;
import rentalmarketplace.repository.ReviewRepository;
import rentalmarketplace.repository.UserRepository;
import rentalmarketplace.security.AccessPolicy;
import rentalmarketplace.service.AuthenticationService;
import rentalmarketplace.service.ExportService;
import rentalmarketplace.service.ListingService;
import rentalmarketplace.service.RentalRequestService;
import rentalmarketplace.service.RequestSearchService;
import rentalmarketplace.service.ReviewService;
import rentalmarketplace.service.StatisticsService;
import rentalmarketplace.service.UserService;

public class AppContext {
  private final AuthenticationService authenticationService;
  private final UserService userService;
  private final ListingService listingService;
  private final RentalRequestService rentalRequestService;
  private final RequestSearchService requestSearchService;
  private final ReviewService reviewService;
  private final StatisticsService statisticsService;
  private final ExportService exportService = new ExportService();

  public AppContext() {
    UserRepository users = new UserRepository();
    authenticationService = new AuthenticationService(users);
    userService = new UserService(users, new AccessPolicy(authenticationService::currentUser));
    listingService = new ListingService(new ListingRepository(), userService);
    rentalRequestService =
        new RentalRequestService(new RentalRequestRepository(), listingService, userService);
    requestSearchService =
        new RequestSearchService(rentalRequestService, listingService, userService);
    reviewService =
        new ReviewService(
            new ReviewRepository(), rentalRequestService, listingService, userService);
    statisticsService =
        new StatisticsService(userService, listingService, rentalRequestService, reviewService);
  }

  public AuthenticationService authenticationService() {
    return authenticationService;
  }

  public AccessPolicy access() {
    return userService().access();
  }

  public UserService userService() {
    return userService;
  }

  public ListingService listingService() {
    return listingService;
  }

  public RentalRequestService rentalRequestService() {
    return rentalRequestService;
  }

  public RequestSearchService requestSearchService() {
    return requestSearchService;
  }

  public ReviewService reviewService() {
    return reviewService;
  }

  public ExportService exportService() {
    return exportService;
  }

  public StatisticsService statisticsService() {
    return statisticsService;
  }

  public FXMLLoader loader(String resourcePath) {
    AppFonts.load();
    URL location = AppContext.class.getResource(resourcePath);
    if (location == null) {
      throw new IllegalArgumentException("Не найден файл интерфейса " + resourcePath);
    }
    FXMLLoader loader = new FXMLLoader(location);
    loader.setControllerFactory(this::createController);
    return loader;
  }

  private Object createController(Class<?> type) {
    try {
      try {
        return type.getConstructor(AppContext.class).newInstance(this);
      } catch (NoSuchMethodException e) {
        return type.getConstructor().newInstance();
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Не удалось создать контроллер " + type.getName(), e);
    }
  }
}
