package foo;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimplestTest {

  @Test
  public void testAdd() throws Exception {
  	assertEquals(Simplest.add(4, 5), 9);
  }
}
