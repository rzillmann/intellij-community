// "Replace with 'Stream.mapToLong().sum()'" "true-preview"

import java.util.List;
import java.util.Map;

public class Main {
  private String s;

  public Main(List<Map<String, String>> s) {
    long count = s.stream().<String>flatMap(map -> map.values().stream()).cou<caret>nt();
  }
}