package org.sonar.squid.recognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CamelCaseDetectorTest {

  @Test
  public void scan() {
    CamelCaseDetector detector = new CamelCaseDetector(0.3);
    assertEquals(1, detector.scan("isDog() or isCat()"));
    assertEquals(0, detector.scan("String name;"));
  }
}
