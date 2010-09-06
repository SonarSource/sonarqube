package org.sonar.java.recognizer;

import java.util.ArrayList;

import org.junit.Test;
import org.sonar.java.recognizer.JavaFootprint;
import org.sonar.squid.recognizer.CodeRecognizer;

import static org.junit.Assert.assertEquals;

public class JavaFootprintTest {

  @Test
  public void codeLineProbability() {
    CodeRecognizer detector = new CodeRecognizer(0.9, new JavaFootprint());
    assertEquals(0.999, detector.recognition("for( int i = 0; i<4; i++) {"), 0.001);
    assertEquals(0.95, detector.recognition("}"), 0.01);
    assertEquals(0.965, detector.recognition("int i;"), 0.01);
    assertEquals(0.95, detector.recognition("for (String line: lines) "), 0.01);
    assertEquals(0.965, detector.recognition("if (true) "), 0.01);
    assertEquals(0.95, detector.recognition("catch (Exception exception) "), 0.01);
    assertEquals(0.99, detector.recognition("try { "), 0.01);
    assertEquals(0.7, detector.recognition("toto && tata"), 0.01);
    assertEquals(0.91, detector.recognition("toto && tata || dupond "), 0.01);
    assertEquals(0.95, detector.recognition(" word word word word word word } "), 0.01);
    assertEquals(0, detector.recognition("Google is a great compagny"), 0.001);
  }

  @Test
  public void extractCodeLines() {
    CodeRecognizer detector = new CodeRecognizer(0.9, new JavaFootprint());
    ArrayList<String> comments = new ArrayList<String>();
    comments.add("for() {");
    comments.add("int i;");
    comments.add("toto tata");
    assertEquals(2, detector.extractCodeLines(comments).size());
    detector = new CodeRecognizer(0.99, new JavaFootprint());
    assertEquals(1, detector.extractCodeLines(comments).size());
  }

  @Test
  public void falsePositifRecognition() {
    CodeRecognizer detector = new CodeRecognizer(0.9, new JavaFootprint());
    ArrayList<String> comments = new ArrayList<String>();
    comments.add("Use case: <tag1>something <tag2>..."); //SONAR-1235
    assertEquals(0, detector.extractCodeLines(comments).size());
  }
}
