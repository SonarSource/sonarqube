package org.sonar.test;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.junit.ComparisonFailure;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonAssert {

  private final String actualJson;
  private JsonComparison comparison;

  private JsonAssert(String actualJson, JsonParser parser, JsonComparator comparator) {
    this.actualJson = actualJson;
    this.comparison = new JsonComparison(parser, comparator);
  }

  public JsonAssert withStrictTimezone() {
    this.comparison.withTimezone();
    return this;
  }

  public JsonAssert withStrictArrayOrder() {
    this.comparison.withStrictArrayOrder();
    return this;
  }

  public JsonAssert ignoreFields(String... ignoredFields) {
    this.comparison.setIgnoredFields(ignoredFields);
    return this;
  }

  public JsonAssert isSimilarTo(String expected) {
    boolean similar = this.comparison.areSimilar(expected, this.actualJson);
    if (!similar) {
      throw new ComparisonFailure("Not a super-set of expected JSON -", pretty(expected), pretty(this.actualJson));
    }
    return this;
  }

  public JsonAssert isSimilarTo(URL expected) {
    return isSimilarTo(urlToString(expected));
  }

  public JsonAssert isNotSimilarTo(String expected) {
    boolean similar = this.comparison.areSimilar(expected, this.actualJson);
    if (similar) {
      throw new ComparisonFailure("It's a super-set of expected JSON -", pretty(expected), pretty(this.actualJson));
    }
    return this;
  }

  public JsonAssert isNotSimilarTo(URL expected) {
    return isNotSimilarTo(urlToString(expected));
  }

  public static JsonAssert assertJson(String actualJson) {
    // Here we instantiate the concrete classes, but in real scenarios, these could be injected
    JsonParser parser = new JsonParser(); // This should be your own JsonParser, not Gson's
    JsonComparator comparator = new StandardJsonComparator(false, false, new HashSet<>());
    return new JsonAssert(actualJson, parser, comparator);
  }

  public static JsonAssert assertJson(URL actualJson) {
    return assertJson(urlToString(actualJson));
  }

  private static String urlToString(URL url) {
    try {
      return IOUtils.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to load JSON from " + url, e);
    }
  }

  private static String pretty(String json) {
    JsonElement element = JsonParser.parseString(json);
    return new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(element);
  }
}

