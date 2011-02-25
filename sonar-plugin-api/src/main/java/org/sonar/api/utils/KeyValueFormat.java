/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.collections.Bag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.RulePriority;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Util class to format key/value data. Output is a string representation ready to be
 * injected into the database
 *
 * @since 1.10
 */
public final class KeyValueFormat<K extends Comparable, V extends Comparable> {

  public static final String PAIR_SEPARATOR = ";";
  public static final String FIELD_SEPARATOR = "=";

  private Converter<K> keyConverter;
  private Converter<V> valueConverter;

  private KeyValueFormat(Converter<K> keyConverter, Converter<V> valueConverter) {
    this.keyConverter = keyConverter;
    this.valueConverter = valueConverter;
  }

  public static abstract class Converter<TYPE> {
    abstract String toString(TYPE type);

    abstract TYPE fromString(String s);
  }

  public static final class StringConverter extends Converter<String> {
    static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    String toString(String s) {
      return s;
    }

    @Override
    String fromString(String s) {
      return s;
    }
  }

  public static final class IntegerConverter extends Converter<Integer> {
    static final IntegerConverter INSTANCE = new IntegerConverter();

    private IntegerConverter() {
    }

    @Override
    String toString(Integer s) {
      return (s == null ? "" : String.valueOf(s));
    }

    @Override
    Integer fromString(String s) {
      return StringUtils.isBlank(s) ? null : NumberUtils.toInt(s);
    }
  }

  public static final class SeverityConverter extends Converter<RulePriority> {
    static final SeverityConverter INSTANCE = new SeverityConverter();

    private SeverityConverter() {
    }

    @Override
    String toString(RulePriority s) {
      return (s == null ? "" : s.toString());
    }

    @Override
    RulePriority fromString(String s) {
      return StringUtils.isBlank(s) ? null : RulePriority.valueOf(s);
    }
  }

  public static final class DoubleConverter extends Converter<Double> {
    static final DoubleConverter INSTANCE = new DoubleConverter();

    private DoubleConverter() {
    }

    @Override
    String toString(Double d) {
      return (d == null ? "" : String.valueOf(d));
    }

    @Override
    Double fromString(String s) {
      return StringUtils.isBlank(s) ? null : NumberUtils.toDouble(s);
    }
  }

  public static class DateConverter extends Converter<Date> {
    private DateFormat dateFormat;

    public DateConverter() {
      this("yyyy-MM-dd");
    }

    DateConverter(String format) {
      this.dateFormat = new SimpleDateFormat(format);
    }

    @Override
    String toString(Date d) {
      return (d == null ? "" : dateFormat.format(d));
    }

    @Override
    Date fromString(String s) {
      try {
        return StringUtils.isBlank(s) ? null : dateFormat.parse(s);
      } catch (ParseException e) {
        throw new SonarException("Not a date: " + s, e);
      }
    }
  }

  public static class DateTimeConverter extends DateConverter {
    public DateTimeConverter() {
      super("yyyy-MM-dd'T'HH:mm:ssZ");
    }
  }

  public static <K extends Comparable, V extends Comparable> KeyValueFormat<K, V> create(Converter<K> keyConverter, Converter<V> valueConverter) {
    return new KeyValueFormat<K, V>(keyConverter, valueConverter);
  }

  public static KeyValueFormat<String, String> createStringString() {
    return new KeyValueFormat<String, String>(StringConverter.INSTANCE, StringConverter.INSTANCE);
  }

  public static KeyValueFormat<String, Date> createStringDate() {
    return new KeyValueFormat<String, Date>(StringConverter.INSTANCE, new DateConverter());
  }

  public static KeyValueFormat<String, Date> createStringDateTime() {
    return new KeyValueFormat<String, Date>(StringConverter.INSTANCE, new DateTimeConverter());
  }

  public static KeyValueFormat<Integer, String> createIntString() {
    return new KeyValueFormat<Integer, String>(IntegerConverter.INSTANCE, StringConverter.INSTANCE);
  }

  public static KeyValueFormat<Integer, Integer> createIntInt() {
    return new KeyValueFormat<Integer, Integer>(IntegerConverter.INSTANCE, IntegerConverter.INSTANCE);
  }

  public static KeyValueFormat<Integer, Double> createIntDouble() {
    return new KeyValueFormat<Integer, Double>(IntegerConverter.INSTANCE, DoubleConverter.INSTANCE);
  }

  public static KeyValueFormat<Integer, Date> createIntDate() {
    return new KeyValueFormat<Integer, Date>(IntegerConverter.INSTANCE, new DateConverter());
  }

  public static KeyValueFormat<Integer, Date> createIntDateTime() {
    return new KeyValueFormat<Integer, Date>(IntegerConverter.INSTANCE, new DateTimeConverter());
  }

  public String toString(Map<K, V> map) {
    return toString(map.entrySet());
  }

  public String toString(Multimap<K, V> multimap) {
    return toString(multimap.entries());
  }

  private String toString(Collection<Map.Entry<K, V>> entries) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<K, V> entry : entries) {
      if (!first) {
        sb.append(PAIR_SEPARATOR);
      }
      sb.append(keyConverter.toString(entry.getKey()));
      sb.append(FIELD_SEPARATOR);
      if (entry.getValue() != null) {
        sb.append(valueConverter.toString(entry.getValue()));
      }
      first = false;
    }
    return sb.toString();
  }

  public SortedMap<K, V> toSortedMap(String data) {
    SortedMap<K, V> map = new TreeMap<K, V>();
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
        String key = keyValue[0];
        String value = (keyValue.length == 2 ? keyValue[1] : "");
        map.put(keyConverter.fromString(key), valueConverter.fromString(value));
      }
    }
    return map;
  }

  public SortedSetMultimap<K, V> toSortedMultimap(String data) {
    SortedSetMultimap<K, V> map = TreeMultimap.create();
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
        String key = keyValue[0];
        String value = (keyValue.length == 2 ? keyValue[1] : "");
        map.put(keyConverter.fromString(key), valueConverter.fromString(value));
      }
    }
    return map;
  }


  /**
   * Transforms a string with the following format : "key1=value1;key2=value2..."
   * into a Map<KEY, VALUE>. Requires to implement the transform(key,value) method
   *
   * @param data        the input string
   * @param transformer the interface to implement
   * @return a Map of <key, value>
   * @deprecated since 2.7. Use instance methods instead of static methods, for example KeyValueFormat.createIntString().parse(String)
   */
  @Deprecated
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
   * @deprecated since 2.7. Use instance methods instead of static methods, for example KeyValueFormat.createIntString().parse(String)
   */
  @Deprecated
  public static Map<String, String> parse(String data) {
    Map<String, String> map = new HashMap<String, String>();
    String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
    for (String pair : pairs) {
      String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
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
   * @deprecated since 2.7. Use instance methods instead of static methods, for example KeyValueFormat.createIntString().parse(String)
   */
  @Deprecated
  public static <KEY, VALUE> String format(Map<KEY, VALUE> map) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!first) {
        sb.append(PAIR_SEPARATOR);
      }
      sb.append(entry.getKey().toString());
      sb.append(FIELD_SEPARATOR);
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
  @Deprecated
  public static String format(Bag bag) {
    return format(bag, 0);
  }

  /**
   * @since 1.11
   * @deprecated use Multiset from google collections instead of commons-collections bags
   */
  @Deprecated
  public static String format(Bag bag, int var) {
    StringBuilder sb = new StringBuilder();
    if (bag != null) {
      boolean first = true;
      for (Object obj : bag.uniqueSet()) {
        if (!first) {
          sb.append(PAIR_SEPARATOR);
        }
        sb.append(obj.toString());
        sb.append(FIELD_SEPARATOR);
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
   * @deprecated since 2.7. Use instance methods instead of static methods, for example KeyValueFormat.createIntString().parse(String)
   */
  @Deprecated
  public static String format(Multiset<?> set) {
    StringBuilder sb = new StringBuilder();
    if (set != null) {
      boolean first = true;
      for (Multiset.Entry<?> entry : set.entrySet()) {
        if (!first) {
          sb.append(PAIR_SEPARATOR);
        }
        sb.append(entry.getElement().toString());
        sb.append(FIELD_SEPARATOR);
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
   * @deprecated since 2.7. Use instance methods instead of static methods, for example KeyValueFormat.createIntString().parse(String)
   */
  @Deprecated
  public static String format(Object... objects) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    if (objects != null) {
      for (int i = 0; i < objects.length; i++) {
        if (!first) {
          sb.append(PAIR_SEPARATOR);
        }
        sb.append(objects[i++].toString());
        sb.append(FIELD_SEPARATOR);
        sb.append(objects[i]);
        first = false;
      }
    }
    return sb.toString();
  }

  @Deprecated
  public interface Transformer<KEY, VALUE> {
    KeyValue<KEY, VALUE> transform(String key, String value);
  }

  /**
   * Implementation of Transformer<String, Double>
   */
  @Deprecated
  public static class StringNumberPairTransformer implements Transformer<String, Double> {
    public KeyValue<String, Double> transform(String key, String value) {
      return new KeyValue<String, Double>(key, toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Double, Double>
   */
  @Deprecated
  public static class DoubleNumbersPairTransformer implements Transformer<Double, Double> {
    public KeyValue<Double, Double> transform(String key, String value) {
      return new KeyValue<Double, Double>(toDouble(key), toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Integer, Integer>
   */
  @Deprecated
  public static class IntegerNumbersPairTransformer implements Transformer<Integer, Integer> {
    public KeyValue<Integer, Integer> transform(String key, String value) {
      return new KeyValue<Integer, Integer>(toInteger(key), toInteger(value));
    }
  }


  /**
   * Implementation of Transformer<RulePriority, Integer>
   */
  @Deprecated
  public static class RulePriorityNumbersPairTransformer implements Transformer<RulePriority, Integer> {

    public KeyValue<RulePriority, Integer> transform(String key, String value) {
      try {
        if (StringUtils.isBlank(value)) {
          value = "0";
        }
        return new KeyValue<RulePriority, Integer>(RulePriority.valueOf(key.toUpperCase()), Integer.parseInt(value));
      } catch (Exception e) {
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
