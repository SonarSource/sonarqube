package org.sonar.squid.recognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegexDetectorTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeProbability() {
    new RegexDetector("toto", -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProbabilityHigherThan1() {
    new RegexDetector("toto", 1.2);
  }

  @Test
  public void testProbability() {
    RegexDetector pattern = new RegexDetector("toto", 0.3);
    assertEquals(0.3, pattern.recognition(" toto "), 0.01);
    assertEquals(0, pattern.recognition("sql"), 0.01);
    assertEquals(1 - Math.pow(0.7, 3), pattern.recognition(" toto toto toto "), 0.01);
  }

  @Test
  public void testSeveralMatches() {
    RegexDetector pattern = new RegexDetector("(\\S\\.\\S)", 0.3); // \S is non-whitespace character 
    assertEquals(0.0, pattern.recognition(" toto "), 0.001);
    assertEquals(0.3, pattern.recognition("abc.def ghi jkl"), 0.001);
    assertEquals(0.51, pattern.recognition("abc.def.ghi"), 0.001);
    assertEquals(0.51, pattern.recognition("abc.def ghi.jkl"), 0.001);
  }
}
