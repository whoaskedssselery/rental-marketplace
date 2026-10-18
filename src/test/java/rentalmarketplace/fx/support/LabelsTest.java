package rentalmarketplace.fx.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rentalmarketplace.model.RentalRequestStatus;
import rentalmarketplace.model.UserRole;
import rentalmarketplace.service.RequestSort;

class LabelsTest {

  @Test
  void everyStatusHasItsOwnRussianName() {
    Set<String> names = new HashSet<>();
    for (RentalRequestStatus status : RentalRequestStatus.values()) {
      String name = Labels.status(status);
      assertNotEquals("", name);
      assertNotEquals(status.name(), name);
      names.add(name);
    }
    assertEquals(RentalRequestStatus.values().length, names.size());
  }

  @Test
  void everySortHasItsOwnRussianName() {
    Set<String> names = new HashSet<>();
    for (RequestSort sort : RequestSort.values()) {
      names.add(Labels.sort(sort));
    }
    assertEquals(RequestSort.values().length, names.size());
    assertEquals("", Labels.sort(null));
  }

  @Test
  void everyRoleHasItsOwnRussianName() {
    Set<String> names = new HashSet<>();
    for (UserRole role : UserRole.values()) {
      names.add(Labels.role(role));
    }
    assertEquals(UserRole.values().length, names.size());
    assertEquals("Владелец", Labels.role(UserRole.OWNER));
  }

  @Test
  void nullValuesGiveEmptyText() {
    assertEquals("", Labels.status(null));
    assertEquals("", Labels.role(null));
    assertEquals("", Labels.date(null));
    assertEquals("", Labels.dateTime(null));
    assertEquals("", Labels.money(null));
  }

  @Test
  void formatsDatesAndMoney() {
    assertEquals("05.09.2026", Labels.date(LocalDate.of(2026, 9, 5)));
    assertEquals("05.09.2026 14:30", Labels.dateTime(LocalDateTime.of(2026, 9, 5, 14, 30)));
    assertEquals("500,00 руб.", Labels.money(new BigDecimal("500")));
    assertEquals("Да", Labels.yesNo(true));
    assertEquals("Нет", Labels.yesNo(false));
  }

  @Test
  void moneyGroupsThousands() {
    String text = Labels.money(new BigDecimal("12500.5"));

    assertEquals("12 500,50 руб.", text.replace(' ', ' ').replace(' ', ' '));
  }
}
