package rentalmarketplace.service;

import java.util.List;

public record SheetData(String name, List<String> headers, List<List<Object>> rows) {}
