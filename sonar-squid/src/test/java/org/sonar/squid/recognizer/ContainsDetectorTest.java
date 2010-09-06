package org.sonar.squid.recognizer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContainsDetectorTest {

  @Test
  public void scan() {
    ContainsDetector detector = new ContainsDetector(0.3, "++", "for(");
    assertEquals(2, detector.scan("for (int i =0; i++; i<4) {"));
    assertEquals(0, detector.scan("String name;"));
  }
}
