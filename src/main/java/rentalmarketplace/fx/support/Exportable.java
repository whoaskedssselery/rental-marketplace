package rentalmarketplace.fx.support;

import java.util.List;

public interface Exportable {
  String exportName();

  List<String> headers();

  List<List<Object>> rows();
}
