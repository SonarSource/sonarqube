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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MapSettingsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PropertyDefinitions definitions;

  @Properties({
    @Property(key = "hello", name = "Hello", defaultValue = "world"),
    @Property(key = "date", name = "Date", defaultValue = "2010-05-18"),
    @Property(key = "datetime", name = "DateTime", defaultValue = "2010-05-18T15:50:45+0100"),
    @Property(key = "boolean", name = "Boolean", defaultValue = "true"),
    @Property(key = "falseboolean", name = "False Boolean", defaultValue = "false"),
    @Property(key = "integer", name = "Integer", defaultValue = "12345"),
    @Property(key = "array", name = "Array", defaultValue = "one,two,three"),
    @Property(key = "multi_values", name = "Array", defaultValue = "1,2,3", multiValues = true),
    @Property(key = "sonar.jira", name = "Jira Server", type = PropertyType.PROPERTY_SET, propertySetKey = "jira"),
    @Property(key = "newKey", name = "New key", deprecatedKey = "oldKey"),
    @Property(key = "newKeyWithDefaultValue", name = "New key with default value", deprecatedKey = "oldKeyWithDefaultValue", defaultValue = "default_value"),
    @Property(key = "new_multi_values", name = "New multi values", defaultValue = "1,2,3", multiValues = true, deprecatedKey = "old_multi_values")
  })
  private static class Init {
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init_definitions() {
    definitions = new PropertyDefinitions();
    definitions.addComponent(Init.class);
  }

  @Test
  public void set_throws_NPE_if_key_is_null() {
    MapSettings underTest = new MapSettings();

    expectKeyNullNPE();

    underTest.set(null, randomAlphanumeric(3));
  }

  @Test
  public void set_throws_NPE_if_value_is_null() {
    MapSettings underTest = new MapSettings();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("value can't be null");

    underTest.set(randomAlphanumeric(3), null);
  }

  @Test
  public void set_accepts_empty_value_and_trims_it() {
    MapSettings underTest = new MapSettings();
    Random random = new Random();
    String key = randomAlphanumeric(3);

    underTest.set(key, blank(random));

    assertThat(underTest.getString(key)).isEmpty();
  }

  @Test
  public void default_values_should_be_loaded_from_definitions() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getDefaultValue("hello")).isEqualTo("world");
  }

  @Test
  @UseDataProvider("setPropertyCalls")
  public void all_setProperty_methods_throws_NPE_if_key_is_null(BiConsumer<Settings, String> setPropertyCaller) {
    Settings settings = new MapSettings();

    expectKeyNullNPE();

    setPropertyCaller.accept(settings, null);
  }

  @Test
  public void set_property_string_throws_NPE_if_key_is_null() {
    String key = randomAlphanumeric(3);

    Settings underTest = new MapSettings(new PropertyDefinitions(singletonList(PropertyDefinition.builder(key).multiValues(true).build())));

    expectKeyNullNPE();

    underTest.setProperty(null, new String[] {"1", "2"});
  }

  private void expectKeyNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");
  }

  @Test
  @UseDataProvider("setPropertyCalls")
  public void all_set_property_methods_trims_key(BiConsumer<Settings, String> setPropertyCaller) {
    Settings underTest = new MapSettings();

    Random random = new Random();
    String blankBefore = blank(random);
    String blankAfter = blank(random);
    String key = randomAlphanumeric(3);

    setPropertyCaller.accept(underTest, blankBefore + key + blankAfter);

    assertThat(underTest.hasKey(key)).isTrue();
  }

  @Test
  public void set_property_string_array_trims_key() {
    String key = randomAlphanumeric(3);

    Settings underTest = new MapSettings(new PropertyDefinitions(singletonList(PropertyDefinition.builder(key).multiValues(true).build())));

    Random random = new Random();
    String blankBefore = blank(random);
    String blankAfter = blank(random);

    underTest.setProperty(blankBefore + key + blankAfter, new String[] {"1", "2"});

    assertThat(underTest.hasKey(key)).isTrue();
  }

  private static String blank(Random random) {
    StringBuilder b = new StringBuilder();
    IntStream.range(0, random.nextInt(3)).mapToObj(s -> " ").forEach(b::append);
    return b.toString();
  }

  @DataProvider
  public static Object[][] setPropertyCalls() {
    List<BiConsumer<Settings, String>> callers = Arrays.asList(
      (settings, key) -> settings.setProperty(key, 123),
      (settings, key) -> settings.setProperty(key, 123L),
      (settings, key) -> settings.setProperty(key, 123.2F),
      (settings, key) -> settings.setProperty(key, 123.2D),
      (settings, key) -> settings.setProperty(key, false),
      (settings, key) -> settings.setProperty(key, new Date()),
      (settings, key) -> settings.setProperty(key, new Date(), true));

    return callers.stream().map(t -> new Object[] {t}).toArray(Object[][]::new);
  }

  @Test
  public void setProperty_methods_trims_value() {
    Settings underTest = new MapSettings();

    Random random = new Random();
    String blankBefore = blank(random);
    String blankAfter = blank(random);
    String key = randomAlphanumeric(3);
    String value = randomAlphanumeric(3);

    underTest.setProperty(key, blankBefore + value + blankAfter);

    assertThat(underTest.getString(key)).isEqualTo(value);
  }

  @Test
  public void set_property_int() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", 123);
    assertThat(settings.getInt("foo")).isEqualTo(123);
    assertThat(settings.getString("foo")).isEqualTo("123");
    assertThat(settings.getBoolean("foo")).isFalse();
  }

  @Test
  public void default_number_values_are_zero() {
    Settings settings = new MapSettings();
    assertThat(settings.getInt("foo")).isEqualTo(0);
    assertThat(settings.getLong("foo")).isEqualTo(0L);
  }

  @Test
  public void getInt_value_must_be_valid() {
    thrown.expect(NumberFormatException.class);

    Settings settings = new MapSettings();
    settings.setProperty("foo", "not a number");
    settings.getInt("foo");
  }

  @Test
  public void all_values_should_be_trimmed_set_property() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "   FOO ");
    assertThat(settings.getString("foo")).isEqualTo("FOO");
  }

  @Test
  public void test_get_default_value() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getDefaultValue("unknown")).isNull();
  }

  @Test
  public void test_get_string() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("hello", "Russia");
    assertThat(settings.getString("hello")).isEqualTo("Russia");
  }

  @Test
  public void setProperty_date() {
    Settings settings = new MapSettings();
    Date date = DateUtils.parseDateTime("2010-05-18T15:50:45+0100");
    settings.setProperty("aDate", date);
    settings.setProperty("aDateTime", date, true);

    assertThat(settings.getString("aDate")).isEqualTo("2010-05-18");
    assertThat(settings.getString("aDateTime")).startsWith("2010-05-18T");
  }

  @Test
  public void test_get_date() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getDate("unknown")).isNull();
    assertThat(settings.getDate("date").getDate()).isEqualTo(18);
    assertThat(settings.getDate("date").getMonth()).isEqualTo(4);
  }

  @Test
  public void test_get_date_not_found() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getDate("unknown")).isNull();
  }

  @Test
  public void test_get_datetime() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getDateTime("unknown")).isNull();
    assertThat(settings.getDateTime("datetime").getDate()).isEqualTo(18);
    assertThat(settings.getDateTime("datetime").getMonth()).isEqualTo(4);
    assertThat(settings.getDateTime("datetime").getMinutes()).isEqualTo(50);
  }

  @Test
  public void test_get_double() {
    Settings settings = new MapSettings();
    settings.setProperty("from_double", 3.14159);
    settings.setProperty("from_string", "3.14159");
    assertThat(settings.getDouble("from_double")).isEqualTo(3.14159, Offset.offset(0.00001));
    assertThat(settings.getDouble("from_string")).isEqualTo(3.14159, Offset.offset(0.00001));
    assertThat(settings.getDouble("unknown")).isNull();
  }

  @Test
  public void test_get_float() {
    Settings settings = new MapSettings();
    settings.setProperty("from_float", 3.14159f);
    settings.setProperty("from_string", "3.14159");
    assertThat(settings.getDouble("from_float")).isEqualTo(3.14159f, Offset.offset(0.00001));
    assertThat(settings.getDouble("from_string")).isEqualTo(3.14159f, Offset.offset(0.00001));
    assertThat(settings.getDouble("unknown")).isNull();
  }

  @Test
  public void test_get_bad_float() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "bar");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property 'foo' is not a float value");
    settings.getFloat("foo");
  }

  @Test
  public void test_get_bad_double() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "bar");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property 'foo' is not a double value");
    settings.getDouble("foo");
  }

  @Test
  public void testSetNullFloat() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", (Float) null);
    assertThat(settings.getFloat("foo")).isNull();
  }

  @Test
  public void testSetNullDouble() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", (Double) null);
    assertThat(settings.getDouble("foo")).isNull();
  }

  @Test
  public void getStringArray() {
    Settings settings = new MapSettings(definitions);
    String[] array = settings.getStringArray("array");
    assertThat(array).isEqualTo(new String[] {"one", "two", "three"});
  }

  @Test
  public void setStringArray() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("multi_values", new String[] {"A", "B"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[] {"A", "B"});
  }

  @Test
  public void setStringArrayTrimValues() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("multi_values", new String[] {" A ", " B "});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[] {"A", "B"});
  }

  @Test
  public void setStringArrayEscapeCommas() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("multi_values", new String[] {"A,B", "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[] {"A,B", "C,D"});
  }

  @Test
  public void setStringArrayWithEmptyValues() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("multi_values", new String[] {"A,B", "", "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[] {"A,B", "", "C,D"});
  }

  @Test
  public void setStringArrayWithNullValues() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("multi_values", new String[] {"A,B", null, "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[] {"A,B", "", "C,D"});
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailToSetArrayValueOnSingleValueProperty() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("array", new String[] {"A", "B", "C"});
  }

  @Test
  public void getStringArray_no_value() {
    Settings settings = new MapSettings();
    String[] array = settings.getStringArray("array");
    assertThat(array).isEmpty();
  }

  @Test
  public void shouldTrimArray() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "  one,  two, three  ");
    String[] array = settings.getStringArray("foo");
    assertThat(array).isEqualTo(new String[] {"one", "two", "three"});
  }

  @Test
  public void shouldKeepEmptyValuesWhenSplitting() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "  one,  , two");
    String[] array = settings.getStringArray("foo");
    assertThat(array).isEqualTo(new String[] {"one", "", "two"});
  }

  @Test
  public void testDefaultValueOfGetString() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getString("hello")).isEqualTo("world");
  }

  @Test
  public void set_property_boolean() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", true);
    settings.setProperty("bar", false);
    assertThat(settings.getBoolean("foo")).isTrue();
    assertThat(settings.getBoolean("bar")).isFalse();
    assertThat(settings.getString("foo")).isEqualTo("true");
    assertThat(settings.getString("bar")).isEqualTo("false");
  }

  @Test
  public void ignore_case_of_boolean_values() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "true");
    settings.setProperty("bar", "TRUE");
    // labels in UI
    settings.setProperty("baz", "True");

    assertThat(settings.getBoolean("foo")).isTrue();
    assertThat(settings.getBoolean("bar")).isTrue();
    assertThat(settings.getBoolean("baz")).isTrue();
  }

  @Test
  public void get_boolean() {
    Settings settings = new MapSettings(definitions);
    assertThat(settings.getBoolean("boolean")).isTrue();
    assertThat(settings.getBoolean("falseboolean")).isFalse();
    assertThat(settings.getBoolean("unknown")).isFalse();
    assertThat(settings.getBoolean("hello")).isFalse();
  }

  @Test
  public void shouldCreateByIntrospectingComponent() {
    Settings settings = new MapSettings();
    settings.getDefinitions().addComponent(MyComponent.class);

    // property definition has been loaded, ie for default value
    assertThat(settings.getDefaultValue("foo")).isEqualTo("bar");
  }

  @Property(key = "foo", name = "Foo", defaultValue = "bar")
  public static class MyComponent {

  }

  @Test
  public void getStringLines_no_value() {
    Assertions.assertThat(new MapSettings().getStringLines("foo")).hasSize(0);
  }

  @Test
  public void getStringLines_single_line() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "the line");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"the line"});
  }

  @Test
  public void getStringLines_linux() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "one\ntwo");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"one", "two"});

    settings.setProperty("foo", "one\ntwo\n");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"one", "two"});
  }

  @Test
  public void getStringLines_windows() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "one\r\ntwo");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"one", "two"});

    settings.setProperty("foo", "one\r\ntwo\r\n");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"one", "two"});
  }

  @Test
  public void getStringLines_mix() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "one\r\ntwo\nthree");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[] {"one", "two", "three"});
  }

  @Test
  public void getKeysStartingWith() {
    Settings settings = new MapSettings();
    settings.setProperty("sonar.jdbc.url", "foo");
    settings.setProperty("sonar.jdbc.username", "bar");
    settings.setProperty("sonar.security", "admin");

    assertThat(settings.getKeysStartingWith("sonar")).containsOnly("sonar.jdbc.url", "sonar.jdbc.username", "sonar.security");
    assertThat(settings.getKeysStartingWith("sonar.jdbc")).containsOnly("sonar.jdbc.url", "sonar.jdbc.username");
    assertThat(settings.getKeysStartingWith("other")).hasSize(0);
  }

  @Test
  public void should_fallback_deprecated_key_to_default_value_of_new_key() {
    Settings settings = new MapSettings(definitions);

    assertThat(settings.getString("newKeyWithDefaultValue")).isEqualTo("default_value");
    assertThat(settings.getString("oldKeyWithDefaultValue")).isEqualTo("default_value");
  }

  @Test
  public void should_fallback_deprecated_key_to_new_key() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("newKey", "value of newKey");

    assertThat(settings.getString("newKey")).isEqualTo("value of newKey");
    assertThat(settings.getString("oldKey")).isEqualTo("value of newKey");
  }

  @Test
  public void should_load_value_of_deprecated_key() {
    // it's used for example when deprecated settings are set through command-line
    Settings settings = new MapSettings(definitions);
    settings.setProperty("oldKey", "value of oldKey");

    assertThat(settings.getString("newKey")).isEqualTo("value of oldKey");
    assertThat(settings.getString("oldKey")).isEqualTo("value of oldKey");
  }

  @Test
  public void should_load_values_of_deprecated_key() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("oldKey", "a,b");

    assertThat(settings.getStringArray("newKey")).containsOnly("a", "b");
    assertThat(settings.getStringArray("oldKey")).containsOnly("a", "b");
  }

  @Test
  public void should_support_deprecated_props_with_multi_values() {
    Settings settings = new MapSettings(definitions);
    settings.setProperty("new_multi_values", new String[] {" A ", " B "});
    assertThat(settings.getStringArray("new_multi_values")).isEqualTo(new String[] {"A", "B"});
    assertThat(settings.getStringArray("old_multi_values")).isEqualTo(new String[] {"A", "B"});
  }
}
