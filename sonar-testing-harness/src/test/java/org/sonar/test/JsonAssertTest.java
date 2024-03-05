package org.sonar.test;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.sonar.test.JsonAssert;

import java.net.URL;

public class JsonAssertTest {

  @Test
  public void testIsSimilarTo_withIdenticalJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test(expected = ComparisonFailure.class)
  public void testIsSimilarTo_withDifferentJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"Jane\", \"age\":25}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test
  public void testIsSimilarTo_withExtraFieldsInActual() {
    String actualJson = "{\"name\":\"John\", \"age\":30, \"city\":\"New York\"}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test
  public void testIsNotSimilarTo_withDifferentJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"Jane\", \"age\":25}";
    JsonAssert.assertJson(actualJson).isNotSimilarTo(expectedJson);
  }

  @Test(expected = ComparisonFailure.class)
  public void testIsNotSimilarTo_withIdenticalJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isNotSimilarTo(expectedJson);
  }

  @Test
  public void testWithStrictArrayOrder() {
    String actualJson = "[\"apple\", \"banana\"]";
    String expectedJson = "[\"banana\", \"apple\"]";
    JsonAssert.assertJson(actualJson)
            .withStrictArrayOrder()
            .isNotSimilarTo(expectedJson); // Should not be similar because the order matters
  }

  @Test
  public void testWithStrictTimezone() {
    String actualJson = "{\"time\": \"2020-01-01T10:00:00+0100\"}";
    String expectedJson = "{\"time\": \"2020-01-01T09:00:00Z\"}";
    JsonAssert.assertJson(actualJson)
            .withStrictTimezone()
            .isNotSimilarTo(expectedJson); // Should not be similar because the timezone matters
  }

  @Test
  public void testIgnoreFields() {
    String actualJson = "{\"name\":\"John\", \"age\":30, \"city\":\"New York\"}";
    String expectedJson = "{\"name\":\"John\", \"city\":\"Los Angeles\"}";
    JsonAssert.assertJson(actualJson)
            .ignoreFields("age")
            .isSimilarTo(expectedJson); // Should be similar because age is ignored
  }

  // Test loading expected JSON from URL
  @Test
  public void testIsSimilarTo_withUrlExpected() throws Exception {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    URL expectedUrl = new URL("http://example.com/expected.json"); // This should point to a real URL for your test
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedUrl);
  }

}

