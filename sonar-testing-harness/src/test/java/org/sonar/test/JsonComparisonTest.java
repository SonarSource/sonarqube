package org.sonar.test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.Collections;
import java.util.HashSet;

public class JsonComparisonTest {

  private JsonParser parser;
  private JsonComparator comparator;
  private JsonComparison jsonComparison;

  @Before
  public void setUp() {
    parser = new SimpleJsonParser();
    comparator = new StandardJsonComparator(false, false, new HashSet<>());
    jsonComparison = new JsonComparison(parser, comparator);
  }

  @Test
  public void testAreSimilar_withIdenticalSimpleJson() {
    String json1 = "{\"name\":\"John\", \"age\":30}";
    String json2 = "{\"name\":\"John\", \"age\":30}";
    assertTrue(jsonComparison.areSimilar(json1, json2));
  }

  @Test
  public void testAreSimilar_withDifferentSimpleJson() {
    String json1 = "{\"name\":\"John\", \"age\":30}";
    String json2 = "{\"name\":\"Jane\", \"age\":25}";
    assertFalse(jsonComparison.areSimilar(json1, json2));
  }

  @Test
  public void testAreSimilar_withIdenticalNestedJson() {
    String json1 = "{\"name\":\"John\", \"location\":{\"city\":\"New York\",\"country\":\"USA\"}}";
    String json2 = "{\"name\":\"John\", \"location\":{\"city\":\"New York\",\"country\":\"USA\"}}";
    assertTrue(jsonComparison.areSimilar(json1, json2));
  }

  @Test
  public void testAreSimilar_withDifferentNestedJson() {
    String json1 = "{\"name\":\"John\", \"location\":{\"city\":\"New York\",\"country\":\"USA\"}}";
    String json2 = "{\"name\":\"John\", \"location\":{\"city\":\"Los Angeles\",\"country\":\"USA\"}}";
    assertFalse(jsonComparison.areSimilar(json1, json2));
  }

  @Test
  public void testAreSimilar_withStrictArrayOrder() {
    comparator = new StandardJsonComparator(false, true, new HashSet<>());
    jsonComparison = new JsonComparison(parser, comparator);

    String json1 = "[\"apple\", \"banana\", \"cherry\"]";
    String json2 = "[\"apple\", \"cherry\", \"banana\"]";
    assertFalse(jsonComparison.areSimilar(json1, json2));
  }

  @Test
  public void testAreSimilar_withLenientArrayOrder() {
    String json1 = "[\"apple\", \"banana\", \"cherry\"]";
    String json2 = "[\"apple\", \"cherry\", \"banana\"]";
    assertTrue(jsonComparison.areSimilar(json1, json2));
  }

  @Test(expected = IllegalStateException.class)
  public void testAreSimilar_withInvalidJson() {
    String json1 = "{\"name\": \"John\""; // Missing closing bracket
    String json2 = "{\"name\":\"John\", \"age\":30}";
    jsonComparison.areSimilar(json1, json2);
  }

}
