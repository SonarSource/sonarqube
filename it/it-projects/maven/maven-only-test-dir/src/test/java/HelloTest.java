import org.junit.Test;

import static org.junit.Assert.assertNotSame;

public class HelloTest {
  @Test
  public void hiho() {
    assertNotSame("hi", "ho");
  }
}
