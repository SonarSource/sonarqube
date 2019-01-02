/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.rules.RulePriority;

/**
 * <p>Formats and parses key/value pairs with the text representation : "key1=value1;key2=value2". Field typing
 * is supported, to make conversion from/to primitive types easier for example.
 * <br>
 * Since version 4.5.1, text keys and values are escaped if they contain the separator characters '=' or ';'.
 * <br>
 * <b>Parsing examples</b>
 * <pre>
 *   Map&lt;String,String&gt; mapOfStrings = KeyValueFormat.parse("hello=world;foo=bar");
 *   Map&lt;String,Integer&gt; mapOfStringInts = KeyValueFormat.parseStringInt("one=1;two=2");
 *   Map&lt;Integer,String&gt; mapOfIntStrings = KeyValueFormat.parseIntString("1=one;2=two");
 *   Map&lt;String,Date&gt; mapOfStringDates = KeyValueFormat.parseStringDate("d1=2014-01-14;d2=2015-07-28");
 *
 *   // custom conversion
 *   Map&lt;String,MyClass&gt; mapOfStringMyClass = KeyValueFormat.parse("foo=xxx;bar=yyy",
 *     KeyValueFormat.newStringConverter(), new MyClassConverter());
 * </pre>
 * <br>
 * <b>Formatting examples</b>
 * <pre>
 *   String output = KeyValueFormat.format(map);
 *
 *   Map&lt;Integer,String&gt; mapIntString;
 *   KeyValueFormat.formatIntString(mapIntString);
 * </pre>
 * @since 1.10
 */
public final class KeyValueFormat {
  public static final String PAIR_SEPARATOR = ";";
  public static final String FIELD_SEPARATOR = "=";

  private KeyValueFormat() {
    // only static methods
  }

  private static class FieldParserContext {
    private final StringBuilder result = new StringBuilder();
    private boolean escaped = false;
    private char firstChar;
    private char previous = (char) -1;
  }

  static class FieldParser {
    private static final char DOUBLE_QUOTE = '"';
    private final String csv;
    private int position = 0;

    FieldParser(String csv) {
      this.csv = csv;
    }

    @CheckForNull
    String nextKey() {
      return next('=');
    }

    @CheckForNull
    String nextVal() {
      return next(';');
    }

    @CheckForNull
    private String next(char separator) {
      if (position >= csv.length()) {
        return null;
      }
      FieldParserContext context = new FieldParserContext();
      context.firstChar = csv.charAt(position);
      // check if value is escaped by analyzing first character
      checkEscaped(context);

      boolean isEnd = false;
      while (position < csv.length() && !isEnd) {
        isEnd = advance(separator, context);
      }
      return context.result.toString();
    }

    private boolean advance(char separator, FieldParserContext context) {
      boolean end = false;
      char c = csv.charAt(position);
      if (c == separator && !context.escaped) {
        end = true;
        position++;
      } else if (c == '\\' && context.escaped && position < csv.length() + 1 && csv.charAt(position + 1) == DOUBLE_QUOTE) {
        // on a backslash that escapes double-quotes -> keep double-quotes and jump after
        context.previous = DOUBLE_QUOTE;
        context.result.append(context.previous);
        position += 2;
      } else if (c == '"' && context.escaped && context.previous != '\\') {
        // on unescaped double-quotes -> end of escaping.
        // assume that next character is a separator (= or ;). This could be
        // improved to enforce check.
        end = true;
        position += 2;
      } else {
        context.result.append(c);
        context.previous = c;
        position++;
      }
      return end;
    }

    private void checkEscaped(FieldParserContext context) {
      if (context.firstChar == DOUBLE_QUOTE) {
        context.escaped = true;
        position++;
        context.previous = context.firstChar;
      }
    }
  }

  public abstract static class Converter<T> {
    abstract String format(@Nullable T type);

    @CheckForNull
    abstract T parse(String s);


    String escape(String s) {
      if (s.contains(FIELD_SEPARATOR) || s.contains(PAIR_SEPARATOR)) {
        return new StringBuilder()
          .append(FieldParser.DOUBLE_QUOTE)
          .append(s.replace("\"", "\\\""))
          .append(FieldParser.DOUBLE_QUOTE).toString();
      }
      return s;
    }
  }

  public static final class StringConverter extends Converter<String> {
    private static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    String format(@Nullable String s) {
      return s == null ? "" : escape(s);
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
    String format(@Nullable Object o) {
      return o == null ? "" : escape(o.toString());
    }

    @Override
    String parse(String s) {
      throw new UnsupportedOperationException("Can not parse with ToStringConverter: " + s);
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
    String format(@Nullable Integer s) {
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
    String format(@Nullable RulePriority s) {
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
    String format(@Nullable Double d) {
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

    private DateConverter(String format) {
      this.dateFormat = new SimpleDateFormat(format);
    }

    @Override
    String format(@Nullable Date d) {
      return d == null ? "" : dateFormat.format(d);
    }

    @Override
    Date parse(String s) {
      try {
        return StringUtils.isBlank(s) ? null : dateFormat.parse(s);
      } catch (ParseException e) {
        throw new IllegalArgumentException("Not a date with format: " + dateFormat.toPattern(), e);
      }
    }
  }

  public static DateConverter newDateConverter() {
    return newDateConverter(DateUtils.DATE_FORMAT);
  }

  public static DateConverter newDateTimeConverter() {
    return newDateConverter(DateUtils.DATETIME_FORMAT);
  }

  public static DateConverter newDateConverter(String format) {
    return new DateConverter(format);
  }

  /**
   * If input is null, then an empty map is returned.
   */
  public static <K, V> Map<K, V> parse(@Nullable String input, Converter<K> keyConverter, Converter<V> valueConverter) {
    Map<K, V> map = new LinkedHashMap<>();
    if (input != null) {
      FieldParser reader = new FieldParser(input);
      boolean end = false;
      while (!end) {
        String key = reader.nextKey();
        if (key == null) {
          end = true;
        } else {
          String val = StringUtils.defaultString(reader.nextVal(), "");
          map.put(keyConverter.parse(key), valueConverter.parse(val));
        }
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

}
