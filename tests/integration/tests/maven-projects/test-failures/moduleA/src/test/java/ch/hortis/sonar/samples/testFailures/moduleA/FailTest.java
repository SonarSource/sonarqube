package ch.hortis.sonar.samples.testFailures.moduleA;

public class FailTest extends junit.framework.TestCase {
	
	public void testAWithFailure() {
	  assertEquals(true, false);
	}
	
	public void testAWithError() {
	  if (true) throw new RuntimeException("Error test");
	}

  public void shouldNotFail() {
    fail();
  }
  
  public void testWithSucces() throws InterruptedException {
    Thread.sleep(5);
    assertTrue(true);
  } 
}