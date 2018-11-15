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
package org.sonar.api.config.internal;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.ArrayUtils;

public class MultivalueProperty {
  private MultivalueProperty() {
    // prevents instantiation
  }

  public static String[] parseAsCsv(String key, String value) {
    return parseAsCsv(key, value, Function.identity());
  }

  public static String[] parseAsCsv(String key, String value, Function<String, String> valueProcessor) {
    String cleanValue = MultivalueProperty.trimFieldsAndRemoveEmptyFields(value);
    List<String> result = new ArrayList<>();
    try (CSVParser csvParser = CSVFormat.RFC4180
      .withHeader((String) null)
      .withIgnoreEmptyLines()
      .withIgnoreSurroundingSpaces()
      .parse(new StringReader(cleanValue))) {
      List<CSVRecord> records = csvParser.getRecords();
      if (records.isEmpty()) {
        return ArrayUtils.EMPTY_STRING_ARRAY;
      }
      processRecords(result, records, valueProcessor);
      return result.toArray(new String[result.size()]);
    } catch (IOException e) {
      throw new IllegalStateException("Property: '" + key + "' doesn't contain a valid CSV value: '" + value + "'", e);
    }
  }

  /**
   * In most cases we expect a single record. <br>Having multiple records means the input value was splitted over multiple lines (this is common in Maven).
   * For example:
   * <pre>
   *   &lt;sonar.exclusions&gt;
   *     src/foo,
   *     src/bar,
   *     src/biz
   *   &lt;sonar.exclusions&gt;
   * </pre>
   * In this case records will be merged to form a single list of items. Last item of a record is appended to first item of next record.
   * <p>
   * This is a very curious case, but we try to preserve line break in the middle of an item:
   * <pre>
   *   &lt;sonar.exclusions&gt;
   *     a
   *     b,
   *     c
   *   &lt;sonar.exclusions&gt;
   * </pre>
   * will produce ['a\nb', 'c']
   */
  private static void processRecords(List<String> result, List<CSVRecord> records, Function<String, String> valueProcessor) {
    for (CSVRecord csvRecord : records) {
      Iterator<String> it = csvRecord.iterator();
      if (!result.isEmpty()) {
        String next = it.next();
        if (!next.isEmpty()) {
          int lastItemIdx = result.size() - 1;
          String previous = result.get(lastItemIdx);
          if (previous.isEmpty()) {
            result.set(lastItemIdx, valueProcessor.apply(next));
          } else {
            result.set(lastItemIdx, valueProcessor.apply(previous + "\n" + next));
          }
        }
      }
      it.forEachRemaining(s -> {
        String apply = valueProcessor.apply(s);
        result.add(apply);
      });
    }
  }

  /**
   * Removes the empty fields from the value of a multi-value property from empty fields, including trimming each field.
   * <p>
   * Quotes can be used to prevent an empty field to be removed (as it is used to preserve empty spaces).
   * <ul>
   *    <li>{@code "" => ""}</li>
   *    <li>{@code " " => ""}</li>
   *    <li>{@code "," => ""}</li>
   *    <li>{@code ",," => ""}</li>
   *    <li>{@code ",,," => ""}</li>
   *    <li>{@code ",a" => "a"}</li>
   *    <li>{@code "a," => "a"}</li>
   *    <li>{@code ",a," => "a"}</li>
   *    <li>{@code "a,,b" => "a,b"}</li>
   *    <li>{@code "a,   ,b" => "a,b"}</li>
   *    <li>{@code "a,\"\",b" => "a,b"}</li>
   *    <li>{@code "\"a\",\"b\"" => "\"a\",\"b\""}</li>
   *    <li>{@code "\"  a  \",\"b \"" => "\"  a  \",\"b \""}</li>
   *    <li>{@code "\"a\",\"\",\"b\"" => "\"a\",\"\",\"b\""}</li>
   *    <li>{@code "\"a\",\"  \",\"b\"" => "\"a\",\"  \",\"b\""}</li>
   *    <li>{@code "\"  a,,b,c  \",\"d \"" => "\"  a,,b,c  \",\"d \""}</li>
   *    <li>{@code "a,\"  \",b" => "ab"]}</li>
   * </ul>
   */
  @VisibleForTesting
  static String trimFieldsAndRemoveEmptyFields(String str) {
    char[] chars = str.toCharArray();
    char[] res = new char[chars.length];
    /*
     * set when reading the first non trimmable char after a separator char (or the beginning of the string)
     * unset when reading a separator
     */
    boolean inField = false;
    boolean inQuotes = false;
    int i = 0;
    int resI = 0;
    for (; i < chars.length; i++) {
      boolean isSeparator = chars[i] == ',';
      if (!inQuotes && isSeparator) {
        // exiting field (may already be unset)
        inField = false;
        if (resI > 0) {
          resI = retroTrim(res, resI);
        }
      } else {
        boolean isTrimmed = !inQuotes && istrimmable(chars[i]);
        if (isTrimmed && !inField) {
          // we haven't meet any non trimmable char since the last separator yet
          continue;
        }

        boolean isEscape = isEscapeChar(chars[i]);
        if (isEscape) {
          inQuotes = !inQuotes;
        }

        // add separator as we already had one field
        if (!inField && resI > 0) {
          res[resI] = ',';
          resI++;
        }

        // register in field (may already be set)
        inField = true;
        // copy current char
        res[resI] = chars[i];
        resI++;
      }
    }
    // inQuotes can only be true at this point if quotes are unbalanced
    if (!inQuotes) {
      // trim end of str
      resI = retroTrim(res, resI);
    }
    return new String(res, 0, resI);
  }

  private static boolean isEscapeChar(char aChar) {
    return aChar == '"';
  }

  private static boolean istrimmable(char aChar) {
    return aChar <= ' ';
  }

  /**
   * Reads from index {@code resI} to the beginning into {@code res} looking up the location of the trimmable char with
   * the lowest index before encountering a non-trimmable char.
   * <p>
   * This basically trims {@code res} from any trimmable char at its end.
   *
   * @return index of next location to put new char in res
   */
  private static int retroTrim(char[] res, int resI) {
    int i = resI;
    while (i >= 1) {
      if (!istrimmable(res[i - 1])) {
        return i;
      }
      i--;
    }
    return i;
  }

}
