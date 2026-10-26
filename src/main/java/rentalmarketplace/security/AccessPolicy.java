package rentalmarketplace.security;

import java.util.Objects;
import java.util.function.Supplier;
import rentalmarketplace.exception.BusinessRuleException;
import rentalmarketplace.model.User;
import rentalmarketplace.model.UserRole;

/** Authorization shared by services and UI; the actor is resolved afresh for each action. */
public final class AccessPolicy {
  private final Supplier<User> actor;

  public AccessPolicy(Supplier<User> actor) {
    this.actor = Objects.requireNonNull(actor);
  }

  public User currentUser() {
    User user = actor.get();
    if (user == null) {
      throw new BusinessRuleException("Войдите в аккаунт");
    }
    return user;
  }

  public boolean isAdmin() {
    return currentUser().getRole() == UserRole.ADMIN;
  }

  public boolean mayEdit(Integer ownerId) {
    User user = currentUser();
    return user.getRole() == UserRole.ADMIN || user.getId().equals(ownerId);
  }

  public void requireOwner(Integer ownerId) {
    if (!mayEdit(ownerId)) {
      throw new BusinessRuleException("Можно изменять только свои данные");
    }
  }

  public void requireAdmin() {
    if (!isAdmin()) {
      throw new BusinessRuleException("Действие доступно только администратору");
    }
  }
}
