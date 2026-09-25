package rentalmarketplace.exception;

public class InputClosedException extends RuntimeException {
  public InputClosedException() {
    super("Ввод закрыт");
  }
}
