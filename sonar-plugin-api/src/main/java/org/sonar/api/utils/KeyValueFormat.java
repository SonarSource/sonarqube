/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.commons.collections.Bag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.rules.RulePriority;

import javax.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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

    boolean requiresEscaping() {
      return false;
    }
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

    @Override
    boolean requiresEscaping() {
      return true;
    }
  }

  public static final class UnescapedStringConverter extends Converter<String> {
    private static final UnescapedStringConverter INSTANCE = new UnescapedStringConverter();

    private UnescapedStringConverter() {
    }

    @Override
    String format(String s) {
      return s;
    }

    @Override
    String parse(String s) {
      return s;
    }

    @Override
    boolean requiresEscaping() {
      return false;
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

    @Override
    boolean requiresEscaping() {
      return true;
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
      return s == null ? "" : String.valueOf(s);
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
      return s == null ? "" : s.toString();
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
      return d == null ? "" : String.valueOf(d);
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
      return d == null ? "" : dateFormat.format(d);
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

  public static <K, V> Map<K, V> parse(@Nullable String data, Converter<K> keyConverter, Converter<V> valueConverter) {
    Map<K, V> map = Maps.newLinkedHashMap();
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        int indexOfEqualSign = pair.indexOf(FIELD_SEPARATOR);
        String key = pair.substring(0, indexOfEqualSign);
        String value = pair.substring(indexOfEqualSign + 1);
        map.put(keyConverter.parse(key), valueConverter.parse(value));
      }
    }
    return map;
  }

  public static Map<String, String> parse(@Nullable String data) {
    return parse(data, newStringConverter(), newStringConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<String, Integer> parseStringInt(@Nullable String data) {
    return parse(data, newStringConverter(), newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<String, Double> parseStringDouble(@Nullable String data) {
    return parse(data, newStringConverter(), newDoubleConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, String> parseIntString(@Nullable String data) {
    return parse(data, newIntegerConverter(), newStringConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Double> parseIntDouble(@Nullable String data) {
    return parse(data, newIntegerConverter(), newDoubleConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Date> parseIntDate(@Nullable String data) {
    return parse(data, newIntegerConverter(), newDateConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Integer> parseIntInt(@Nullable String data) {
    return parse(data, newIntegerConverter(), newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Map<Integer, Date> parseIntDateTime(@Nullable String data) {
    return parse(data, newIntegerConverter(), newDateTimeConverter());
  }

  /**
   * Value of pairs is the occurrences of the same single key. A multiset is sometimes called a bag.
   * For example parsing "foo=2;bar=1" creates a multiset with 3 elements : foo, foo and bar.
   */
  /**
   * @since 2.7
   */
  public static <K> Multiset<K> parseMultiset(@Nullable String data, Converter<K> keyConverter) {
    // to keep the same order
    Multiset<K> multiset = LinkedHashMultiset.create();
    if (data != null) {
      String[] pairs = StringUtils.split(data, PAIR_SEPARATOR);
      for (String pair : pairs) {
        String[] keyValue = StringUtils.split(pair, FIELD_SEPARATOR);
        String key = keyValue[0];
        String value = keyValue.length == 2 ? keyValue[1] : "0";
        multiset.add(keyConverter.parse(key), new IntegerConverter().parse(value));
      }
    }
    return multiset;
  }


  /**
   * @since 2.7
   */
  public static Multiset<Integer> parseIntegerMultiset(@Nullable String data) {
    return parseMultiset(data, newIntegerConverter());
  }

  /**
   * @since 2.7
   */
  public static Multiset<String> parseMultiset(@Nullable String data) {
    return parseMultiset(data, newStringConverter());
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
}
