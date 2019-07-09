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
package org.sonar.server.config;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.config.PropertyDefinition.builder;

@RunWith(DataProviderRunner.class)
public class ConfigurationProviderTest {
  private static final String[] EMPTY_STRING_ARRAY = {};
  private final String nonDeclaredKey = RandomStringUtils.randomAlphabetic(3);
  private final String nonMultivalueKey = RandomStringUtils.randomAlphabetic(3);
  private final String multivalueKey = RandomStringUtils.randomAlphabetic(3);
  private final MapSettings settings = new MapSettings(new PropertyDefinitions(
    builder(nonMultivalueKey).multiValues(false).build(),
    builder(multivalueKey).multiValues(true).build()));

  private ConfigurationProvider underTest = new ConfigurationProvider();

  @Test
  @UseDataProvider("trimFieldsAndEncodedCommas")
  public void getStringArray_split_on_comma_trim_and_support_encoded_comma_as_Settings_getStringArray(String idemUseCase) {
    settings.setProperty(nonDeclaredKey, idemUseCase);
    settings.setProperty(nonMultivalueKey, idemUseCase);
    settings.setProperty(multivalueKey, idemUseCase);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorIsTheSame(configuration, nonDeclaredKey);
    getStringArrayBehaviorIsTheSame(configuration, nonMultivalueKey);
    getStringArrayBehaviorIsTheSame(configuration, multivalueKey);
  }

  @DataProvider
  public static Object[][] trimFieldsAndEncodedCommas() {
    return new Object[][] {
      {"a"},
      {" a"},
      {"a "},
      {" a "},
      {"a,b"},
      {" a,b"},
      {" a ,b"},
      {" a , b"},
      {"a ,b"},
      {" a, b"},
      {"a,b "},
      {" a , b "},
      {"a%2Cb"},
      {"a%2Cb,c"}
    };
  }

  @Test
  @UseDataProvider("emptyStrings")
  public void getStringArray_parses_empty_string_the_same_Settings_if_unknown_or_non_multivalue_property(String emptyValue) {
    settings.setProperty(nonDeclaredKey, emptyValue);
    settings.setProperty(nonMultivalueKey, emptyValue);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorIsTheSame(configuration, nonDeclaredKey);
    getStringArrayBehaviorIsTheSame(configuration, nonMultivalueKey);
  }

  @Test
  @UseDataProvider("emptyStrings")
  public void getStringArray_parses_empty_string_differently_from_Settings_ifmultivalue_property(String emptyValue) {
    settings.setProperty(multivalueKey, emptyValue);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, multivalueKey, EMPTY_STRING_ARRAY);
  }

  @DataProvider
  public static Object[][] emptyStrings() {
    return new Object[][] {
      {""},
      {" "},
      {"    "},
    };
  }

  @Test
  @UseDataProvider("subsequentCommas1")
  public void getStringArray_on_unknown_or_non_multivalue_properties_ignores_subsequent_commas_as_Settings(String subsequentCommas) {
    settings.setProperty(nonDeclaredKey, subsequentCommas);
    settings.setProperty(nonMultivalueKey, subsequentCommas);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorIsTheSame(configuration, nonDeclaredKey);
    getStringArrayBehaviorIsTheSame(configuration, nonMultivalueKey);
  }

  @DataProvider
  public static Object[][] subsequentCommas1() {
    return new Object[][] {
      {",,a"},
      {",,,a"},
      {"a,,b"},
      {"a,,,b,c,,d"}
    };
  }

  @Test
  @UseDataProvider("subsequentCommas2")
  public void getStringArray_on_unknown_or_non_multivalue_properties_ignores_subsequent_commas_differently_from_Settings(String subsequentCommas, String[] expected) {
    settings.setProperty(nonDeclaredKey, subsequentCommas);
    settings.setProperty(nonMultivalueKey, subsequentCommas);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, nonDeclaredKey, expected);
    getStringArrayBehaviorDiffers(configuration, nonMultivalueKey, expected);
  }

  @DataProvider
  public static Object[][] subsequentCommas2() {
    return new Object[][] {
      {",,", EMPTY_STRING_ARRAY},
      {",,,", EMPTY_STRING_ARRAY},
      {"a,,", arrayOf("a")},
      {"a,,,", arrayOf("a")}
    };
  }

  @Test
  @UseDataProvider("subsequentCommas3")
  public void getStringArray_on_multivalue_properties_ignores_subsequent_commas_differently_from_Settings(String subsequentCommas, String[] expected) {
    settings.setProperty(multivalueKey, subsequentCommas);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, multivalueKey, expected);
  }

  @DataProvider
  public static Object[][] subsequentCommas3() {
    return new Object[][] {
      {",,", EMPTY_STRING_ARRAY},
      {",,,", EMPTY_STRING_ARRAY},
      {"a,,", arrayOf("a")},
      {"a,,,", arrayOf("a")},
      {",,a", arrayOf("a")},
      {",,,a", arrayOf("a")},
      {"a,,b", arrayOf("a", "b")},
      {"a,,,b", arrayOf("a", "b")},
      {"a,,,b,,", arrayOf("a", "b")},
      {",,a,,b", arrayOf("a", "b")},
    };
  }

  @Test
  @UseDataProvider("emptyFields1")
  public void getStringArray_on_unknown_or_non_multivalue_properties_ignores_empty_fields_same_as_settings(String emptyFields, String[] expected) {
    settings.setProperty(nonDeclaredKey, emptyFields);
    settings.setProperty(nonMultivalueKey, emptyFields);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorIsTheSame(configuration, nonDeclaredKey, expected);
    getStringArrayBehaviorIsTheSame(configuration, nonMultivalueKey, expected);
  }

  @DataProvider
  public static Object[][] emptyFields1() {
    return new Object[][] {
      // these are inconsistent behaviors of StringUtils.splitByWholeSeparator used under the hood by Settings
      {" , a", arrayOf("a")},
      {" ,a,b", arrayOf("a", "b")},
      {" ,,a,b", arrayOf("a", "b")}
    };
  }

  @Test
  @UseDataProvider("emptyFields2")
  public void getStringArray_on_unknown_or_non_multivalue_properties_ignores_empty_fields_differently_from_settings(String emptyFields, String[] expected) {
    settings.setProperty(nonDeclaredKey, emptyFields);
    settings.setProperty(nonMultivalueKey, emptyFields);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, nonDeclaredKey, expected);
    getStringArrayBehaviorDiffers(configuration, nonMultivalueKey, expected);
  }

  @DataProvider
  public static Object[][] emptyFields2() {
    return new Object[][] {
      {", ", EMPTY_STRING_ARRAY},
      {" ,", EMPTY_STRING_ARRAY},
      {" , ", EMPTY_STRING_ARRAY},
      {" , \n ,, \t", EMPTY_STRING_ARRAY},
      {" , \t ,   , ", EMPTY_STRING_ARRAY},
      {"a, ", arrayOf("a")},
      {"  a, ,", arrayOf("a")},
      {" ,  a, ,", arrayOf("a")},
      {"a,b, ", arrayOf("a", "b")},
      {"a, ,b", arrayOf("a", "b")},
      {" ,a, ,b", arrayOf("a", "b")},
      {" ,,a, ,b", arrayOf("a", "b")},
      {" ,a, ,,b", arrayOf("a", "b")},
      {" ,,a, ,,b", arrayOf("a", "b")},
      {"a, ,b, ", arrayOf("a", "b")},
      {"\t ,a, ,b, ", arrayOf("a", "b")},
    };
  }

  @Test
  @UseDataProvider("emptyFields3")
  public void getStringArray_on_multivalue_properties_ignores_empty_fields_differently_from_settings(String emptyFields, String[] expected) {
    settings.setProperty(multivalueKey, emptyFields);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, multivalueKey, expected);
  }

  @DataProvider
  public static Object[][] emptyFields3() {
    return new Object[][] {
      {", ", EMPTY_STRING_ARRAY},
      {" ,", EMPTY_STRING_ARRAY},
      {" , ", EMPTY_STRING_ARRAY},
      {" , \n ,, \t", EMPTY_STRING_ARRAY},
      {" , \t ,   , ", EMPTY_STRING_ARRAY},
      {"a, ", arrayOf("a")},
      {"  a, ,", arrayOf("a")},
      {" ,  a, ,", arrayOf("a")},
      {"a,b, ", arrayOf("a", "b")},
      {" ,a,b", arrayOf("a", "b")},
      {" ,,a,b", arrayOf("a", "b")},
      {"a, ,b", arrayOf("a", "b")},
      {" ,a, ,b", arrayOf("a", "b")},
      {" ,,a, ,b", arrayOf("a", "b")},
      {" ,a, ,,b", arrayOf("a", "b")},
      {" ,,a, ,,b", arrayOf("a", "b")},
      {"a, ,b, ", arrayOf("a", "b")},
      {"\t ,a, ,b, ", arrayOf("a", "b")},
    };
  }

  @Test
  @UseDataProvider("quotedStrings1")
  public void getStringArray_supports_quoted_strings_when_settings_does_not(String str,
    String[] configurationExpected, String[] settingsExpected) {
    settings.setProperty(nonDeclaredKey, str);
    settings.setProperty(nonMultivalueKey, str);
    settings.setProperty(multivalueKey, str);

    Configuration configuration = underTest.provide(settings);

    getStringArrayBehaviorDiffers(configuration, nonDeclaredKey, configurationExpected, settingsExpected);
    getStringArrayBehaviorDiffers(configuration, nonMultivalueKey, configurationExpected, settingsExpected);
    getStringArrayBehaviorDiffers(configuration, multivalueKey, configurationExpected, settingsExpected);
  }

  @DataProvider
  public static Object[][] quotedStrings1() {
    return new Object[][] {
      {"\"\"", arrayOf(""), arrayOf("\"\"")},
      {" \"\"", arrayOf(""), arrayOf("\"\"")},
      {"\"\" ", arrayOf(""), arrayOf("\"\"")},
      {" \"\" ", arrayOf(""), arrayOf("\"\"")},
      {"\" \"", arrayOf(" "), arrayOf("\" \"")},
      {"   \" \" ", arrayOf(" "), arrayOf("\" \"")},
      {"\"a \"", arrayOf("a "), arrayOf("\"a \"")},
      {"\" a\"", arrayOf(" a"), arrayOf("\" a\"")},
      {"\",\"", arrayOf(","), arrayOf("\"", "\"")},
      // escaped quote in quoted field
      {"\"\"\"\"", arrayOf("\""), arrayOf("\"\"\"\"")},
      {"\"a\"\"\"", arrayOf("a\""), arrayOf("\"a\"\"\"")},
      {"\"\"\"b\"", arrayOf("\"b"), arrayOf("\"\"\"b\"")},
      {"\"a\"\"b\"", arrayOf("a\"b"), arrayOf("\"a\"\"b\"")},
      {"\",\",\"a\"", arrayOf(",", "a"), arrayOf("\"", "\"", "\"a\"")},
    };
  }

  private void getStringArrayBehaviorIsTheSame(Configuration configuration, String key) {
    assertThat(configuration.getStringArray(key))
      .isEqualTo(settings.getStringArray(key));
  }

  private void getStringArrayBehaviorIsTheSame(Configuration configuration, String key, String[] expected) {
    assertThat(configuration.getStringArray(key))
      .isEqualTo(expected)
      .isEqualTo(settings.getStringArray(key));
  }

  private void getStringArrayBehaviorDiffers(Configuration configuration, String key, String[] expected) {
    assertThat(configuration.getStringArray(key))
      .isEqualTo(expected)
      .isNotEqualTo(settings.getStringArray(key));
  }

  private void getStringArrayBehaviorDiffers(Configuration configuration, String key, String[] configurationExpected, String[] settingsExpected) {
    String[] conf = configuration.getStringArray(key);
    String[] sett = settings.getStringArray(key);
    assertThat(conf).isEqualTo(configurationExpected);
    assertThat(sett).isEqualTo(settingsExpected);
    assertThat(conf).isNotEqualTo(sett);
  }

  private static String[] arrayOf(String... strs) {
    return strs;
  }
}
