package org.sonar.squid.recognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeywordsDetectorTest {

  @Test
  public void scan() {
    KeywordsDetector detector = new KeywordsDetector(0.3, "public", "static");
    assertEquals(2, detector.scan("public static void main"));
    assertEquals(1, detector.scan("private(static} String name;"));
    assertEquals(0, detector.scan("publicstatic"));
    assertEquals(0, detector.scan("i++;"));
    detector = new KeywordsDetector(0.3, true, "PUBLIC");
    assertEquals(2, detector.scan("Public static pubLIC"));
  }
}
