package ch.hortis.sonar.samples.testFailures.moduleB;

import ch.hortis.sonar.samples.testFailures.moduleA.*;

public class SuccessTest extends junit.framework.TestCase {
	
	public void testA() {
		ClassA a = new ClassA();
		assertEquals(3, a.a());
	}
	
}