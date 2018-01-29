/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.config;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.MultivalueProperty.parseAsCsv;
import static org.sonar.core.config.MultivalueProperty.trimFieldsAndRemoveEmptyFields;

@RunWith(DataProviderRunner.class)
public class MultivaluePropertyTest {
  private static final String[] EMPTY_STRING_ARRAY = {};

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @UseDataProvider("testParseAsCsv")
  public void parseAsCsv_for_coverage(String value, String[] expected) {
    // parseAsCsv is extensively tested in org.sonar.server.config.ConfigurationProviderTest
    assertThat(parseAsCsv("key", value))
      .isEqualTo(parseAsCsv("key", value, Function.identity()))
      .isEqualTo(expected);
  }

  @Test
  public void parseAsCsv_fails_with_ISE_if_value_can_not_be_parsed() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property: 'multi' doesn't contain a valid CSV value: '\"a ,b'");

    parseAsCsv("multi", "\"a ,b");
  }

  @DataProvider
  public static Object[][] testParseAsCsv() {
    return new Object[][] {
      {"a", arrayOf("a")},
      {" a", arrayOf("a")},
      {"a ", arrayOf("a")},
      {" a, b", arrayOf("a", "b")},
      {"a,b ", arrayOf("a", "b")},
      {"a,,,b,c,,d", arrayOf("a", "b", "c", "d")},
      {" , \n ,, \t", EMPTY_STRING_ARRAY},
      {"\" a\"", arrayOf(" a")},
      {"\",\"", arrayOf(",")},
      // escaped quote in quoted field
      {"\"\"\"\"", arrayOf("\"")}
    };
  }

  private static String[] arrayOf(String... strs) {
    return strs;
  }

  @Test
  public void trimFieldsAndRemoveEmptyFields_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);

    trimFieldsAndRemoveEmptyFields(null);
  }

  @Test
  @UseDataProvider("plains")
  public void trimFieldsAndRemoveEmptyFields_ignores_EmptyFields(String str) {
    assertThat(trimFieldsAndRemoveEmptyFields("")).isEqualTo("");
    assertThat(trimFieldsAndRemoveEmptyFields(str)).isEqualTo(str);

    assertThat(trimFieldsAndRemoveEmptyFields(',' + str)).isEqualTo(str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ',')).isEqualTo(str);
    assertThat(trimFieldsAndRemoveEmptyFields(",,," + str)).isEqualTo(str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ",,,")).isEqualTo(str);

    assertThat(trimFieldsAndRemoveEmptyFields(str + ',' + str)).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ",,," + str)).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(',' + str + ',' + str)).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields("," + str + ",,," + str)).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(",,," + str + ",,," + str)).isEqualTo(str + ',' + str);

    assertThat(trimFieldsAndRemoveEmptyFields(str + ',' + str + ',')).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ",,," + str + ",")).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ",,," + str + ",,")).isEqualTo(str + ',' + str);

    assertThat(trimFieldsAndRemoveEmptyFields(',' + str + ',' + str + ',')).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(",," + str + ',' + str + ',')).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(',' + str + ",," + str + ',')).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(',' + str + ',' + str + ",,")).isEqualTo(str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(",,," + str + ",,," + str + ",,")).isEqualTo(str + ',' + str);

    assertThat(trimFieldsAndRemoveEmptyFields(str + ',' + str + ',' + str)).isEqualTo(str + ',' + str + ',' + str);
    assertThat(trimFieldsAndRemoveEmptyFields(str + ',' + str + ',' + str)).isEqualTo(str + ',' + str + ',' + str);
  }

  @DataProvider
  public static Object[][] plains() {
    return new Object[][] {
      {randomAlphanumeric(1)},
      {randomAlphanumeric(2)},
      {randomAlphanumeric(3 + new Random().nextInt(5))}
    };
  }

  @Test
  @UseDataProvider("emptyAndtrimmable")
  public void trimFieldsAndRemoveEmptyFields_ignores_empty_fields_and_trims_fields(String empty, String trimmable) {
    String expected = trimmable.trim();
    assertThat(empty.trim()).isEmpty();

    assertThat(trimFieldsAndRemoveEmptyFields(trimmable)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ',' + empty)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ",," + empty)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(empty + ',' + trimmable)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(empty + ",," + trimmable)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(empty + ',' + trimmable + ',' + empty)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(empty + ",," + trimmable + ",,," + empty)).isEqualTo(expected);

    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ',' + empty + ',' + empty)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ",," + empty + ",,," + empty)).isEqualTo(expected);

    assertThat(trimFieldsAndRemoveEmptyFields(empty + ',' + empty + ',' + trimmable)).isEqualTo(expected);
    assertThat(trimFieldsAndRemoveEmptyFields(empty + ",,,," + empty + ",," + trimmable)).isEqualTo(expected);

    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ',' + trimmable)).isEqualTo(expected + ',' + expected);
    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + ',' + trimmable + ',' + trimmable)).isEqualTo(expected + ',' + expected + ',' + expected);
    assertThat(trimFieldsAndRemoveEmptyFields(trimmable + "," + trimmable + ',' + trimmable)).isEqualTo(expected + ',' + expected + ',' + expected);
  }

  @Test
  public void trimAccordingToStringTrim() {
    String str = randomAlphanumeric(4);
    for (int i = 0; i <= ' '; i++) {
      String prefixed = (char) i + str;
      String suffixed = (char) i + str;
      String both = (char) i + str + (char) i;
      assertThat(trimFieldsAndRemoveEmptyFields(prefixed)).isEqualTo(prefixed.trim());
      assertThat(trimFieldsAndRemoveEmptyFields(suffixed)).isEqualTo(suffixed.trim());
      assertThat(trimFieldsAndRemoveEmptyFields(both)).isEqualTo(both.trim());
    }
  }

  @DataProvider
  public static Object[][] emptyAndtrimmable() {
    Random random = new Random();
    String oneEmpty = randomTrimmedChars(1, random);
    String twoEmpty = randomTrimmedChars(2, random);
    String threePlusEmpty = randomTrimmedChars(3 + random.nextInt(5), random);
    String onePlusEmpty = randomTrimmedChars(1 + random.nextInt(5), random);

    String plain = randomAlphanumeric(1);
    String plainWithtrimmable = randomAlphanumeric(2) + onePlusEmpty + randomAlphanumeric(3);
    String quotedWithSeparator = '"' + randomAlphanumeric(3) + ',' + randomAlphanumeric(2) + '"';
    String quotedWithDoubleSeparator = '"' + randomAlphanumeric(3) + ",," + randomAlphanumeric(2) + '"';
    String quotedWithtrimmable = '"' + randomAlphanumeric(3) + onePlusEmpty + randomAlphanumeric(2) + '"';

    String[] empties = {oneEmpty, twoEmpty, threePlusEmpty};
    String[] strings = {plain, plainWithtrimmable,
      onePlusEmpty + plain, plain + onePlusEmpty, onePlusEmpty + plain + onePlusEmpty,
      onePlusEmpty + plainWithtrimmable, plainWithtrimmable + onePlusEmpty, onePlusEmpty + plainWithtrimmable + onePlusEmpty,
      onePlusEmpty + quotedWithSeparator, quotedWithSeparator + onePlusEmpty, onePlusEmpty + quotedWithSeparator + onePlusEmpty,
      onePlusEmpty + quotedWithDoubleSeparator, quotedWithDoubleSeparator + onePlusEmpty, onePlusEmpty + quotedWithDoubleSeparator + onePlusEmpty,
      onePlusEmpty + quotedWithtrimmable, quotedWithtrimmable + onePlusEmpty, onePlusEmpty + quotedWithtrimmable + onePlusEmpty
    };

    Object[][] res = new Object[empties.length * strings.length][2];
    int i = 0;
    for (String empty : empties) {
      for (String string : strings) {
        res[i][0] = empty;
        res[i][1] = string;
        i++;
      }
    }
    return res;
  }

  @Test
  @UseDataProvider("emptys")
  public void trimFieldsAndRemoveEmptyFields_quotes_allow_to_preserve_fields(String empty) {
    String quotedEmpty = '"' + empty + '"';

    assertThat(trimFieldsAndRemoveEmptyFields(quotedEmpty)).isEqualTo(quotedEmpty);
    assertThat(trimFieldsAndRemoveEmptyFields(',' + quotedEmpty)).isEqualTo(quotedEmpty);
    assertThat(trimFieldsAndRemoveEmptyFields(quotedEmpty + ',')).isEqualTo(quotedEmpty);
    assertThat(trimFieldsAndRemoveEmptyFields(',' + quotedEmpty + ',')).isEqualTo(quotedEmpty);

    assertThat(trimFieldsAndRemoveEmptyFields(quotedEmpty + ',' + quotedEmpty)).isEqualTo(quotedEmpty + ',' + quotedEmpty);
    assertThat(trimFieldsAndRemoveEmptyFields(quotedEmpty + ",," + quotedEmpty)).isEqualTo(quotedEmpty + ',' + quotedEmpty);

    assertThat(trimFieldsAndRemoveEmptyFields(quotedEmpty + ',' + quotedEmpty + ',' + quotedEmpty)).isEqualTo(quotedEmpty + ',' + quotedEmpty + ',' + quotedEmpty);
  }

  @DataProvider
  public static Object[][] emptys() {
    Random random = new Random();
    return new Object[][] {
      {randomTrimmedChars(1, random)},
      {randomTrimmedChars(2, random)},
      {randomTrimmedChars(3 + random.nextInt(5), random)}
    };
  }

  @Test
  public void trimFieldsAndRemoveEmptyFields_supports_escaped_quote_in_quotes() {
    assertThat(trimFieldsAndRemoveEmptyFields("\"f\"\"oo\"")).isEqualTo("\"f\"\"oo\"");
    assertThat(trimFieldsAndRemoveEmptyFields("\"f\"\"oo\",\"bar\"\"\"")).isEqualTo("\"f\"\"oo\",\"bar\"\"\"");
  }

  @Test
  public void trimFieldsAndRemoveEmptyFields_does_not_fail_on_unbalanced_quotes() {
    assertThat(trimFieldsAndRemoveEmptyFields("\"")).isEqualTo("\"");
    assertThat(trimFieldsAndRemoveEmptyFields("\"foo")).isEqualTo("\"foo");
    assertThat(trimFieldsAndRemoveEmptyFields("foo\"")).isEqualTo("foo\"");

    assertThat(trimFieldsAndRemoveEmptyFields("\"foo\",\"")).isEqualTo("\"foo\",\"");
    assertThat(trimFieldsAndRemoveEmptyFields("\",\"foo\"")).isEqualTo("\",\"foo\"");

    assertThat(trimFieldsAndRemoveEmptyFields("\"foo\",\",  ")).isEqualTo("\"foo\",\",  ");

    assertThat(trimFieldsAndRemoveEmptyFields(" a ,,b , c,  \"foo\",\"  ")).isEqualTo("a,b,c,\"foo\",\"  ");
    assertThat(trimFieldsAndRemoveEmptyFields("\" a ,,b , c,  ")).isEqualTo("\" a ,,b , c,  ");
  }

  private static final char[] SOME_PRINTABLE_TRIMMABLE_CHARS = {
    ' ', '\t', '\n', '\r'
  };

  /**
   * Result of randomTrimmedChars being used as arguments to JUnit test method through the DataProvider feature, they
   * are printed to surefire report. Some of those chars breaks the parsing of the surefire report during sonar analysis.
   * Therefor, we only use a subset of the trimmable chars.
   */
  private static String randomTrimmedChars(int length, Random random) {
    char[] chars = new char[length];
    for (int i = 0; i < chars.length; i++) {
      chars[i] = SOME_PRINTABLE_TRIMMABLE_CHARS[random.nextInt(SOME_PRINTABLE_TRIMMABLE_CHARS.length)];
    }
    return new String(chars);
  }
}
