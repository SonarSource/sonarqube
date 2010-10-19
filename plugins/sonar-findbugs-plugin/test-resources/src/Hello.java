import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

class Hello {

  static String name;
  
  public void methodWithViolations(String n) {
    name = n;
    Collections.sort(Arrays.asList(new Integer[]{1, 2, 3}), new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
        return o1 - o2;
      }
    });
  }
  
  @Override
  public boolean equals(Object obj) {
    return false;
  }
}