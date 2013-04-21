/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.commons.collections.Bag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.RulePriority;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>Formats and parses key/value pairs with the string representation : "key1=value1;key2=value2". Conversion
 * of fields is supported and can be extended.</p>
 *
 * <p>This format can easily be parsed with Ruby code: <code>hash=Hash[*(my_string.split(';').map { |elt| elt.split('=') }.flatten)]</code></p>
 *
 * @since 1.10
 */
public final class KeyValueFormat {

  public static final String PAIR_SEPARATOR = ";";
  public static final String FIELD_SEPARATOR = "=";

  private KeyValueFormat() {
    // only static methods
  }

  public abstract static class Converter<T> {
    abstract String format(T type);

    abstract T parse(String s);
  }

  public static final class StringConverter extends Converter<String> {
    private static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    String format(String s) {
      return s;
    }

    @Override
    String parse(String s) {
      return s;
    }
  }

  public static StringConverter newStringConverter() {
    return StringConverter.INSTANCE;
  }

  public static final class ToStringConverter extends Converter<Object> {
    private static final ToStringConverter INSTANCE = new ToStringConverter();

    private ToStringConverter() {
    }

    @Override
    String format(Object o) {
      return o.toString();
    }

    @Override
    String parse(String s) {
      throw new IllegalStateException("Can not parse with ToStringConverter: " + s);
    }
  }

  public static ToStringConverter newToStringConverter() {
    return ToStringConverter.INSTANCE;
  }

  public static final class IntegerConverter extends Converter<Integer> {
    private static final IntegerConverter INSTANCE = new IntegerConverter();

    private IntegerConverter() {
    }

    @Override
    String format(Integer s) {
      return (s == null ? "" : String.valueOf(s));
    }

    @Override
    Integer parse(String s) {
      return StringUtils.isBlank(s) ? null : NumberUtils.toInt(s);
    }
  }

  public static IntegerConverter newIntegerConverter() {
    return IntegerConverter.INSTANCE;
  }

  public static final class PriorityConverter extends Converter<RulePriority> {
    private static final PriorityConverter INSTANCE = new PriorityConverter();

    private PriorityConverter() {
    }

    @Override
    String format(RulePriority s) {
      return (s == null ? "" : s.toString());
    }

    @Override
    RulePriority parse(String s) {
      return StringUtils.isBlank(s) ? null : RulePriority.valueOf(s);
    }
  }

  public static PriorityConverter newPriorityConverter() {
    return PriorityConverter.INSTANCE;
  }

  public static final class DoubleConverter extends Converter<Double> {
    private static final DoubleConverter INSTANCE = new DoubleConverter();

    private DoubleConverter() {
    }

    @Override
    String format(Double d) {
      return (d == null ? "" : String.valueOf(d));
    }

    @Override
    Double parse(String s) {
      return StringUtils.isBlank(s) ? null : NumberUtils.toDouble(s);
    }
  }

  public static DoubleConverter newDoubleConverter() {
    return DoubleConverter.INSTANCE;
  }

  public static class DateConverter extends Converter<Date> {
    private SimpleDateFormat dateFormat;

    /**
     * @deprecated in version 2.13. Replaced by {@link org.sonar.api.utils.KeyValueFormat#newDateConverter()}
     */
    @Deprecated
    public DateConverter() {
      this(DateUtils.DATE_FORMAT);
    }

    private DateConverter(String format) {
      this.dateFormat = new SimpleDateFormat(format);
    }

    @Override
    String format(Date d) {
      return (d == null ? "" : dateFormat.format(d));
    }

    @Override
    Date parse(String s) {
      try {
        return StringUtils.isBlank(s) ? null : dateFormat.parse(s);
      } catch (ParseException e) {
        throw new SonarException("Not a date with format: " + dateFormat.toPattern(), e);
      }
    }
  }

  public static DateConverter newDateConverter() {
    return new DateConverter(DateUtils.DATE_FORMAT);
  }

  public static DateConverter newDateTimeConverter() {
    return new DateConverter(DateUtils.DATETIME_FORMAT);
  }

  public static DateConverter newDateConverter(String format) {
    return new DateConverter(format);
  }

  /**
   * @deprecated in version 2.13. Replaced by {@link org.sonar.api.utils.KeyValueFormat#newDateTimeConverter()}
   */
  @Deprecated
  public static class DateTimeConverter extends DateConverter {
    public DateTimeConverter() {
      super(DateUtils.DATETIME_FORMAT);
    }
  }

  public static <K, V> Map<K, V> parse(String data, Converter<K> keyConverter, Converter<V> valueConverter) {
    Map<K, V> map = Maps.newLinkedHashMap();
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
        String key = keyValue[0];
        String value = (keyValue.length == 2 ? keyValue[1] : "");
        map.put(keyConverter.parse(key), valueConverter.parse(value));
      }
    }
    return map;
  }

  public static Map<String, String> parse(String data) {
    return parse(data, newStringConverter(), newStringConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<String, Integer> parseStringInt(String data) {
    return parse(data, newStringConverter(), newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<String, Double> parseStringDouble(String data) {
    return parse(data, newStringConverter(), newDoubleConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, String> parseIntString(String data) {
    return parse(data, newIntegerConverter(), newStringConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Double> parseIntDouble(String data) {
    return parse(data, newIntegerConverter(), newDoubleConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Date> parseIntDate(String data) {
    return parse(data, newIntegerConverter(), newDateConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Integer> parseIntInt(String data) {
    return parse(data, newIntegerConverter(), newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Date> parseIntDateTime(String data) {
    return parse(data, newIntegerConverter(), newDateTimeConverter());
  }

  /**
   * Value of pairs is the occurrences of the same single key. A multiset is sometimes called a bag.
   * For example parsing "foo=2;bar=1" creates a multiset with 3 elements : foo, foo and bar.
   */
  /**
   * @since 2.7
   */
  public static <K> Multiset<K> parseMultiset(String data, Converter<K> keyConverter) {
    Multiset<K> multiset = LinkedHashMultiset.create();// to keep the same order
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
        String key = keyValue[0];
        String value = (keyValue.length == 2 ? keyValue[1] : "0");
        multiset.add(keyConverter.parse(key), new IntegerConverter().parse(value));
      }
    }
    return multiset;
  }


  /**
   * @since 2.7
   */
  public static Multiset<Integer> parseIntegerMultiset(String data) {
    return parseMultiset(data, newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Multiset<String> parseMultiset(String data) {
    return parseMultiset(data, newStringConverter());
  }

  /**
   * Transforms a string with the following format: "key1=value1;key2=value2..."
   * into a Map<KEY, VALUE>. Requires to implement the transform(key,value) method
   *
   * @param data        the input string
   * @param transformer the interface to implement
   * @return a Map of <key, value>
   * @deprecated since 2.7
   */
  @Deprecated
  public static <K, V> Map<K, V> parse(String data, Transformer<K, V> transformer) {
    Map<String, String> rawData = parse(data);
    Map<K, V> map = new HashMap<K, V>();
    for (Map.Entry<String, String> entry : rawData.entrySet()) {
      KeyValue<K, V> keyVal = transformer.transform(entry.getKey(), entry.getValue());
      if (keyVal != null) {
        map.put(keyVal.getKey(), keyVal.getValue());
      }
    }
    return map;
  }

  private static <K, V> String formatEntries(Collection<Map.Entry<K, V>> entries, Converter<K> keyConverter, Converter<V> valueConverter) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<K, V> entry : entries) {
      if (!first) {
        sb.append(PAIR_SEPARATOR);
      }
      sb.append(keyConverter.format(entry.getKey()));
      sb.append(FIELD_SEPARATOR);
      if (entry.getValue() != null) {
        sb.append(valueConverter.format(entry.getValue()));
      }
      first = false;
    }
    return sb.toString();
  }

  private static <K> String formatEntries(Set<Multiset.Entry<K>> entries, Converter<K> keyConverter) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Multiset.Entry<K> entry : entries) {
      if (!first) {
        sb.append(PAIR_SEPARATOR);
      }
      sb.append(keyConverter.format(entry.getElement()));
      sb.append(FIELD_SEPARATOR);
      sb.append(new IntegerConverter().format(entry.getCount()));
      first = false;
    }
    return sb.toString();
  }


  /**
   * @since 2.7
   */
  public static <K, V> String format(Map<K, V> map, Converter<K> keyConverter, Converter<V> valueConverter) {
    return formatEntries(map.entrySet(), keyConverter, valueConverter);
  }

  /**
   * @since 2.7
   */
  public static String format(Map map) {
    return format(map, newToStringConverter(), newToStringConverter());
  }

  /**
   * @since 2.7
   */
  public static String formatIntString(Map<Integer, String> map) {
    return format(map, newIntegerConverter(), newStringConverter());
  }

  /**
   * @since 2.7
   */
  public static String formatIntDouble(Map<Integer, Double> map) {
    return format(map, newIntegerConverter(), newDoubleConverter());
  }

  /**
   * @since 2.7
   */
  public static String formatIntDate(Map<Integer, Date> map) {
    return format(map, newIntegerConverter(), newDateConverter());
  }

  /**
   * @since 2.7
   */
  public static String formatIntDateTime(Map<Integer, Date> map) {
    return format(map, newIntegerConverter(), newDateTimeConverter());
  }

  /**
   * @since 2.7
   */
  public static String formatStringInt(Map<String, Integer> map) {
    return format(map, newStringConverter(), newIntegerConverter());
  }

  /**
   * Limitation: there's currently no methods to parse into Multimap.
   *
   * @since 2.7
   */
  public static <K, V> String format(Multimap<K, V> map, Converter<K> keyConverter, Converter<V> valueConverter) {
    return formatEntries(map.entries(), keyConverter, valueConverter);
  }

  /**
   * @since 2.7
   */
  public static <K> String format(Multiset<K> multiset, Converter<K> keyConverter) {
    return formatEntries(multiset.entrySet(), keyConverter);
  }

  public static String format(Multiset multiset) {
    return formatEntries(multiset.entrySet(), newToStringConverter());
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
   * @deprecated since 2.7. Replaced by Converter
   */
  @Deprecated
  public interface Transformer<KEY, VALUE> {
    KeyValue<KEY, VALUE> transform(String key, String value);
  }

  /**
   * Implementation of Transformer<String, Double>
   *
   * @deprecated since 2.7 replaced by Converter
   */
  @Deprecated
  public static class StringNumberPairTransformer implements Transformer<String, Double> {
    public KeyValue<String, Double> transform(String key, String value) {
      return new KeyValue<String, Double>(key, toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Double, Double>
   *
   * @deprecated since 2.7. Replaced by Converter
   */
  @Deprecated
  public static class DoubleNumbersPairTransformer implements Transformer<Double, Double> {
    public KeyValue<Double, Double> transform(String key, String value) {
      return new KeyValue<Double, Double>(toDouble(key), toDouble(value));
    }
  }

  /**
   * Implementation of Transformer<Integer, Integer>
   *
   * @deprecated since 2.7. Replaced by Converter
   */
  @Deprecated
  public static class IntegerNumbersPairTransformer implements Transformer<Integer, Integer> {
    public KeyValue<Integer, Integer> transform(String key, String value) {
      return new KeyValue<Integer, Integer>(toInteger(key), toInteger(value));
    }
  }


  /**
   * Implementation of Transformer<RulePriority, Integer>
   *
   * @deprecated since 2.7. Replaced by Converter
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
