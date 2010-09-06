package org.sonar.squid.recognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndWithDetectorTest {

  @Test
  public void scan() {
    EndWithDetector detector = new EndWithDetector(0.3, '}');
    assertEquals(1, detector.scan(" return true; }"));
    assertEquals(0, detector.scan("} catch(NullPointerException e) {"));
    assertEquals(1, detector.scan("} "));
  }
}
