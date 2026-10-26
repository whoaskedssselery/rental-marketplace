package rentalmarketplace.fx.support;

import java.util.ArrayList;
import java.util.List;

public final class ValidationResult {
  public record FieldError(String field, String message) {
    public String text() {
      return field + ": " + message;
    }
  }

  private final List<FieldError> errors = new ArrayList<>();

  public void add(String field, String message) {
    errors.add(new FieldError(field, message));
  }

  public boolean isValid() {
    return errors.isEmpty();
  }

  public List<FieldError> errors() {
    return List.copyOf(errors);
  }

  public String summary() {
    return String.join("\n", errors.stream().map(FieldError::text).toList());
  }
}
