/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import com.google.common.collect.Multiset;
import org.apache.commons.collections.Bag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.rules.RulePriority;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Util class to format key/value data. Output is a string representation ready to be
 * injected into the database
 *
 * @since 1.10
 */
public final class KeyValueFormat {

  private KeyValueFormat() {
  }

  /**
   * Transforms a string with the following format : "key1=value1;key2=value2..."
   * into a Map<KEY, VALUE>. Requires to implement the transform(key,value) method
   *
   * @param data        the input string
   * @param transformer the interface to implement
   * @return a Map of <key, value>
   */
  public static <KEY, VALUE> Map<KEY, VALUE> parse(String data, Transformer<KEY, VALUE> transformer) {
    Map<String, String> rawData = parse(data);
    Map<KEY, VALUE> map = new HashMap<KEY, VALUE>();
    for (Map.Entry<String, String> entry : rawData.entrySet()) {
      KeyValue<KEY, VALUE> keyVal = transformer.transform(entry.getKey(), entry.getValue());
      if (keyVal != null) {
        map.put(keyVal.getKey(), keyVal.getValue());
      }
    }
    return map;
  }

  /**
   * Transforms a string with the following format : "key1=value1;key2=value2..."
   * into a Map<String,String>
   *
   * @param data the string to parse
   * @return a map
   */
  public static Map<String, String> parse(String data) {
    Map<String, String> map = new HashMap<String, String>();
    String[] pairs = StringUtils.split(data, ";");
    for (String pair : pairs) {
      String[] keyValue = StringUtils.split(pair, "=");
      String key = keyValue[0];
      String value = (keyValue.length == 2 ? keyValue[1] : "");
      map.put(key, value);
    }
    return map;
  }

  /**
   * Transforms a map<KEY,VALUE> into a string with the format : "key1=value1;key2=value2..."
   *
   * @param map the map to transform
   * @return the formatted string
   */
  public static <KEY, VALUE> String format(Map<KEY, VALUE> map) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!first) {
        sb.append(";");
      }
      sb.append(entry.getKey().toString());
      sb.append("=");
      if (entry.getValue() != null) {
        sb.append(entry.getValue());
      }
      first = false;
    }

    return sb.toString();
  }

  /**
   * @since 1.11
   * @deprecated use Multiset from google collections instead of commons-collections bags
   */
  public static String format(Bag bag) {
    return format(bag, 0);
  }

  /**
   * @since 1.11
   * @deprecated use Multiset from google collections instead of commons-collections bags
   */
  public static String format(Bag bag, int var) {
    StringBuilder sb = new StringBuilder();
    if (bag != null) {
      boolean first = true;
      for (Object obj : bag.uniqueSet()) {
        if (!first) {
          sb.append(";");
        }
        sb.append(obj.toString());
        sb.append("=");
        sb.append(bag.getCount(obj) + var);
        first = false;
      }
    }
    return sb.toString();
  }

  /**
   * Transforms a Multiset<?> into a string with the format : "key1=count1;key2=count2..."
   *
   * @param set the set to transform
   * @return the formatted string
   */
  public static String format(Multiset<?> set) {
    StringBuilder sb = new StringBuilder();
    if (set != null) {
      boolean first = true;
      for (Multiset.Entry<?> entry : set.entrySet()) {
        if (!first) {
          sb.append(";");
        }
        sb.append(entry.getElement().toString());
        sb.append("=");
        sb.append(entry.getCount());
        first = false;
      }
    }
    return sb.toString();
  }

  /**
   * Transforms a Object... into a string with the format : "object1=object2;object3=object4..."
   *
   * @param objects the object list to transform
   * @return the formatted string
   */
  public static String format(Object... objects) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    if (objects != null) {
      for (int i = 0; i < objects.length; i++) {
        if (!first) {
          sb.append(";");
        }
        sb.append(objects[i++].toString());
        sb.append("=");
        sb.append(objects[i]);
        first = false;
      }
    }
    return sb.toString();
  }

  public interface Transformer<KEY, VALUE> {
    KeyValue<KEY, VALUE> transform(String key, String value);
  }

  /**
   * Implementation of Transformer<String, Double>
   */
  public static class StringNumberPairTransformer implements Transformer<String, Double> {

    public KeyValue<String, Double> transform(String key, String value) {
      return new KeyValue<String, Double>(key, toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Double, Double>
   */
  public static class DoubleNumbersPairTransformer implements Transformer<Double, Double> {

    public KeyValue<Double, Double> transform(String key, String value) {
      return new KeyValue<Double, Double>(toDouble(key), toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Integer, Integer>
   */
  public static class IntegerNumbersPairTransformer implements Transformer<Integer, Integer> {

    public KeyValue<Integer, Integer> transform(String key, String value) {
      return new KeyValue<Integer, Integer>(toInteger(key), toInteger(value));
    }
  }

  /**
   * Implementation of Transformer<RulePriority, Integer>
   */
  public static class RulePriorityNumbersPairTransformer implements Transformer<RulePriority, Integer> {

    public KeyValue<RulePriority, Integer> transform(String key, String value) {
      try {
        if (StringUtils.isBlank(value)) {
          value = "0";
        }
        return new KeyValue<RulePriority, Integer>(RulePriority.valueOf(key.toUpperCase()), Integer.parseInt(value));
      }
      catch (Exception e) {
        LoggerFactory.getLogger(RulePriorityNumbersPairTransformer.class).warn("Property " + key + " has invalid value: " + value, e);
        return null;
      }
    }
  }

  private static Double toDouble(String value) {
    return StringUtils.isBlank(value) ? null : NumberUtils.toDouble(value);
  }

  private static Integer toInteger(String value) {
    return StringUtils.isBlank(value) ? null : NumberUtils.toInt(value);
  }
}
