package org.sonar.test;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

interface JsonParser {
  Object parse(String json) throws ParseException;
}

interface JsonComparator {
  boolean compare(Object json1, Object json2);
}

class SimpleJsonParser implements JsonParser {
  @Override
  public Object parse(String json) throws ParseException {
    try {
      JSONParser parser = new JSONParser();
      return parser.parse(json);
    } catch (Exception e) {
      throw new ParseException("Invalid JSON: " + json, 0);
    }
  }
}

class StandardJsonComparator implements JsonComparator {
  boolean strictTimezone;
  boolean strictArrayOrder;
  Set<String> ignoredFields;

  public StandardJsonComparator(boolean strictTimezone, boolean strictArrayOrder, Set<String> ignoredFields) {
    this.strictTimezone = strictTimezone;
    this.strictArrayOrder = strictArrayOrder;
    this.ignoredFields = new HashSet<>(ignoredFields);
  }

  @Override
  public boolean compare(Object json1, Object json2) {
    if (json1 == null) {
      return json2 == null;
    }
    if (json2 == null) {
      return false; // json1 is not null but json2 is
    }
    if (json1.getClass() != json2.getClass()) {
      return false;
    }
    if (json1 instanceof JSONArray) {
      return compareArrays((JSONArray) json1, (JSONArray) json2);
    }
    if (json1 instanceof JSONObject) {
      return compareObjects((JSONObject) json1, (JSONObject) json2);
    }
    if (json1 instanceof String) {
      return compareStrings((String) json1, (String) json2);
    }
    if (json1 instanceof Number) {
      return compareNumbers((Number) json1, (Number) json2);
    }
    if (json1 instanceof Boolean) {
      return json1.equals(json2);
    }
    return false;
  }

  private boolean compareArrays(JSONArray expected, JSONArray actual) {
    if (strictArrayOrder) {
      if (expected.size() != actual.size()) {
        return false;
      }
      for (int i = 0; i < expected.size(); i++) {
        if (!compare(expected.get(i), actual.get(i))) {
          return false;
        }
      }
      return true;
    } else {
      // Non-strict array order comparison
      List<Object> unmatchedElements = new ArrayList<>(actual);
      for (Object element : expected) {
        boolean found = false;
        for (Object actualElement : unmatchedElements) {
          if (compare(element, actualElement)) {
            unmatchedElements.remove(actualElement);
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean compareObjects(JSONObject expected, JSONObject actual) {
    if (expected.size() != actual.size()) {
      return false;
    }
    for (Object key : expected.keySet()) {
      if (!actual.containsKey(key) || !compare(expected.get(key), actual.get(key))) {
        return false;
      }
    }
    return true;
  }

  private boolean compareStrings(String expected, String actual) {
    if (!strictTimezone) {
      Date expectedDate = tryParseDate(expected);
      Date actualDate = tryParseDate(actual);
      if (expectedDate != null && actualDate != null) {
        return expectedDate.getTime() == actualDate.getTime();
      }
    }
    return expected.equals(actual);
  }

  private boolean compareNumbers(Number expected, Number actual) {
    return Math.abs(expected.doubleValue() - actual.doubleValue()) < 0.00001;
  }

  @CheckForNull
  private Date tryParseDate(String s) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
    } catch (ParseException ignored) {
      return null; // Not a date
    }
  }
}

class JsonComparison {
  private JsonParser parser;
  private JsonComparator comparator;

  public JsonComparison(JsonParser parser, JsonComparator comparator) {
    this.parser = parser;
    this.comparator = comparator;
  }

  public JsonComparison(com.google.gson.JsonParser parser, JsonComparator comparator) {
    this.comparator = comparator;
  }

  boolean isStrictTimezone() {
    return this.comparator instanceof StandardJsonComparator && ((StandardJsonComparator) this.comparator).strictTimezone;
  }

  JsonComparison withTimezone() {
    if (this.comparator instanceof StandardJsonComparator) {
      ((StandardJsonComparator) this.comparator).strictTimezone = true;
    }
    return this;
  }

  boolean isStrictArrayOrder() {
    return this.comparator instanceof StandardJsonComparator && ((StandardJsonComparator) this.comparator).strictArrayOrder;
  }

  JsonComparison withStrictArrayOrder() {
    if (this.comparator instanceof StandardJsonComparator) {
      ((StandardJsonComparator) this.comparator).strictArrayOrder = true;
    }
    return this;
  }

  JsonComparison setIgnoredFields(String... fields) {
    if (this.comparator instanceof StandardJsonComparator) {
      ((StandardJsonComparator) this.comparator).ignoredFields.clear();
      Collections.addAll(((StandardJsonComparator) this.comparator).ignoredFields, fields);
    }
    return this;
  }

  boolean areSimilar(String expected, String actual) {
    try {
      Object expectedJson = parser.parse(expected);
      Object actualJson = parser.parse(actual);
      return comparator.compare(expectedJson, actualJson);
    } catch (ParseException e) {
      throw new IllegalStateException("Invalid JSON", e);
    }
  }
}

