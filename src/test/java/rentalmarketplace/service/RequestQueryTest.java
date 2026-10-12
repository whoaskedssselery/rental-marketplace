package rentalmarketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RequestQueryTest {

  @Test
  void allHasNoRestrictions() {
    RequestQuery query = RequestQuery.all();

    assertNull(query.text());
    assertNull(query.status());
    assertNull(query.ownerId());
    assertNull(query.listingId());
    assertNull(query.renterId());
    assertNull(query.periodFrom());
    assertNull(query.periodTo());
    assertNull(query.sort());
    assertFalse(query.descending());
  }

  @Test
  void allReturnsEqualQueries() {
    assertEquals(RequestQuery.all(), RequestQuery.all());
  }
}
