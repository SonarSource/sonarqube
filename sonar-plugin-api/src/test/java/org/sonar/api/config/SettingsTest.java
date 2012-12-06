/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.config;

import com.google.common.collect.ImmutableMap;
import org.fest.assertions.Delta;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

import static org.fest.assertions.Assertions.assertThat;

public class SettingsTest {

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
  static class Init {
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void initDefinitions() {
    definitions = new PropertyDefinitions();
    definitions.addComponent(Init.class);
  }

  @Test
  public void defaultValuesShouldBeLoadedFromDefinitions() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDefaultValue("hello")).isEqualTo("world");
  }

  @Test
  public void setProperty_int() {
    Settings settings = new Settings();
    settings.setProperty("foo", 123);
    assertThat(settings.getInt("foo")).isEqualTo(123);
    assertThat(settings.getString("foo")).isEqualTo("123");
    assertThat(settings.getBoolean("foo")).isFalse();
  }

  @Test
  public void setProperty_boolean() {
    Settings settings = new Settings();
    settings.setProperty("foo", true);
    settings.setProperty("bar", false);
    assertThat(settings.getBoolean("foo")).isTrue();
    assertThat(settings.getBoolean("bar")).isFalse();
    assertThat(settings.getString("foo")).isEqualTo("true");
    assertThat(settings.getString("bar")).isEqualTo("false");
  }

  @Test
  public void default_number_values_are_zero() {
    Settings settings = new Settings();
    assertThat(settings.getInt("foo")).isEqualTo(0);
    assertThat(settings.getLong("foo")).isEqualTo(0L);
  }

  @Test
  public void getInt_value_must_be_valid() {
    thrown.expect(NumberFormatException.class);

    Settings settings = new Settings();
    settings.setProperty("foo", "not a number");
    settings.getInt("foo");
  }

  @Test
  public void allValuesShouldBeTrimmed_set_property() {
    Settings settings = new Settings();
    settings.setProperty("foo", "   FOO ");
    assertThat(settings.getString("foo")).isEqualTo("FOO");
  }

  @Test
  public void allValuesShouldBeTrimmed_set_properties() {
    Settings settings = new Settings();
    settings.setProperties(ImmutableMap.of("foo", "  FOO "));
    assertThat(settings.getString("foo")).isEqualTo("FOO");
  }

  @Test
  public void testGetDefaultValue() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDefaultValue("unknown")).isNull();
  }

  @Test
  public void testGetString() {
    Settings settings = new Settings(definitions);
    settings.setProperty("hello", "Russia");
    assertThat(settings.getString("hello")).isEqualTo("Russia");
  }

  @Test
  public void testGetDate() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDate("unknown")).isNull();
    assertThat(settings.getDate("date").getDate()).isEqualTo(18);
    assertThat(settings.getDate("date").getMonth()).isEqualTo(4);
  }

  @Test
  public void testGetDateNotFound() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDate("unknown")).isNull();
  }

  @Test
  public void testGetDateTime() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDateTime("unknown")).isNull();
    assertThat(settings.getDateTime("datetime").getDate()).isEqualTo(18);
    assertThat(settings.getDateTime("datetime").getMonth()).isEqualTo(4);
    assertThat(settings.getDateTime("datetime").getMinutes()).isEqualTo(50);
  }

  @Test
  public void testGetDouble() {
    Settings settings = new Settings();
    settings.setProperty("from_double", 3.14159);
    settings.setProperty("from_string", "3.14159");
    assertThat(settings.getDouble("from_double")).isEqualTo(3.14159, Delta.delta(0.00001));
    assertThat(settings.getDouble("from_string")).isEqualTo(3.14159, Delta.delta(0.00001));
    assertThat(settings.getDouble("unknown")).isNull();
  }

  @Test
  public void testGetFloat() {
    Settings settings = new Settings();
    settings.setProperty("from_float", 3.14159f);
    settings.setProperty("from_string", "3.14159");
    assertThat(settings.getDouble("from_float")).isEqualTo(3.14159f, Delta.delta(0.00001));
    assertThat(settings.getDouble("from_string")).isEqualTo(3.14159f, Delta.delta(0.00001));
    assertThat(settings.getDouble("unknown")).isNull();
  }

  @Test
  public void testGetBadFloat() {
    Settings settings = new Settings();
    settings.setProperty("foo", "bar");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property 'foo' is not a float value");
    settings.getFloat("foo");
  }

  @Test
  public void testGetBadDouble() {
    Settings settings = new Settings();
    settings.setProperty("foo", "bar");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The property 'foo' is not a double value");
    settings.getDouble("foo");
  }

  @Test
  public void testSetNullFloat() {
    Settings settings = new Settings();
    settings.setProperty("foo", (Float) null);
    assertThat(settings.getFloat("foo")).isNull();
  }

  @Test
  public void testSetNullDouble() {
    Settings settings = new Settings();
    settings.setProperty("foo", (Double) null);
    assertThat(settings.getDouble("foo")).isNull();
  }

  @Test
  public void getStringArray() {
    Settings settings = new Settings(definitions);
    String[] array = settings.getStringArray("array");
    assertThat(array).isEqualTo(new String[]{"one", "two", "three"});
  }

  @Test
  public void setStringArray() {
    Settings settings = new Settings(definitions);
    settings.setProperty("multi_values", new String[]{"A", "B"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[]{"A", "B"});
  }

  @Test
  public void setStringArrayTrimValues() {
    Settings settings = new Settings(definitions);
    settings.setProperty("multi_values", new String[]{" A ", " B "});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[]{"A", "B"});
  }

  @Test
  public void setStringArrayEscapeCommas() {
    Settings settings = new Settings(definitions);
    settings.setProperty("multi_values", new String[]{"A,B", "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[]{"A,B", "C,D"});
  }

  @Test
  public void setStringArrayWithEmptyValues() {
    Settings settings = new Settings(definitions);
    settings.setProperty("multi_values", new String[]{"A,B", "", "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[]{"A,B", "", "C,D"});
  }

  @Test
  public void setStringArrayWithNullValues() {
    Settings settings = new Settings(definitions);
    settings.setProperty("multi_values", new String[]{"A,B", null, "C,D"});
    String[] array = settings.getStringArray("multi_values");
    assertThat(array).isEqualTo(new String[]{"A,B", "", "C,D"});
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailToSetArrayValueOnSingleValueProperty() {
    Settings settings = new Settings(definitions);
    settings.setProperty("array", new String[]{"A", "B", "C"});
  }

  @Test
  public void getStringArray_no_value() {
    Settings settings = new Settings();
    String[] array = settings.getStringArray("array");
    assertThat(array).isEmpty();
  }

  @Test
  public void shouldTrimArray() {
    Settings settings = new Settings();
    settings.setProperty("foo", "  one,  two, three  ");
    String[] array = settings.getStringArray("foo");
    assertThat(array).isEqualTo(new String[]{"one", "two", "three"});
  }

  @Test
  public void shouldKeepEmptyValuesWhenSplitting() {
    Settings settings = new Settings();
    settings.setProperty("foo", "  one,  , two");
    String[] array = settings.getStringArray("foo");
    assertThat(array).isEqualTo(new String[]{"one", "", "two"});
  }

  @Test
  public void testDefaultValueOfGetString() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getString("hello")).isEqualTo("world");
  }

  @Test
  public void testGetBoolean() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getBoolean("boolean")).isTrue();
    assertThat(settings.getBoolean("falseboolean")).isFalse();
    assertThat(settings.getBoolean("unknown")).isFalse();
    assertThat(settings.getBoolean("hello")).isFalse();
  }

  @Test
  public void shouldCreateByIntrospectingComponent() {
    Settings settings = Settings.createForComponent(MyComponent.class);

    // property definition has been loaded, ie for default value
    assertThat(settings.getDefaultValue("foo")).isEqualTo("bar");
  }

  @Property(key = "foo", name = "Foo", defaultValue = "bar")
  public static class MyComponent {

  }

  @Test
  public void cloneSettings() {
    Settings target = new Settings(definitions).setProperty("foo", "bar");
    Settings settings = new Settings(target);

    assertThat(settings.getString("foo")).isEqualTo("bar");
    assertThat(settings.getDefinitions()).isSameAs(definitions);

    // do not propagate changes
    settings.setProperty("foo", "changed");
    settings.setProperty("new", "value");

    assertThat(settings.getString("foo")).isEqualTo("changed");
    assertThat(settings.getString("new")).isEqualTo("value");
    assertThat(target.getString("foo")).isEqualTo("bar");
    assertThat(target.getString("new")).isNull();
  }

  @Test
  public void getStringLines_no_value() {
    assertThat(new Settings().getStringLines("foo")).hasSize(0);
  }

  @Test
  public void getStringLines_single_line() {
    Settings settings = new Settings();
    settings.setProperty("foo", "the line");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"the line"});
  }

  @Test
  public void getStringLines_linux() {
    Settings settings = new Settings();
    settings.setProperty("foo", "one\ntwo");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"one", "two"});

    settings.setProperty("foo", "one\ntwo\n");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"one", "two"});
  }

  @Test
  public void getStringLines_windows() {
    Settings settings = new Settings();
    settings.setProperty("foo", "one\r\ntwo");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"one", "two"});

    settings.setProperty("foo", "one\r\ntwo\r\n");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"one", "two"});
  }

  @Test
  public void getStringLines_mix() {
    Settings settings = new Settings();
    settings.setProperty("foo", "one\r\ntwo\nthree");
    assertThat(settings.getStringLines("foo")).isEqualTo(new String[]{"one", "two", "three"});
  }

  @Test
  public void getKeysStartingWith() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.url", "foo");
    settings.setProperty("sonar.jdbc.username", "bar");
    settings.setProperty("sonar.security", "admin");

    assertThat(settings.getKeysStartingWith("sonar")).containsOnly("sonar.jdbc.url", "sonar.jdbc.username", "sonar.security");
    assertThat(settings.getKeysStartingWith("sonar.jdbc")).containsOnly("sonar.jdbc.url", "sonar.jdbc.username");
    assertThat(settings.getKeysStartingWith("other")).hasSize(0);
  }

  @Test
  public void should_fallback_deprecated_key_to_default_value_of_new_key() {
    Settings settings = new Settings(definitions);

    assertThat(settings.getString("newKeyWithDefaultValue")).isEqualTo("default_value");
    assertThat(settings.getString("oldKeyWithDefaultValue")).isEqualTo("default_value");
  }

  @Test
  public void should_fallback_deprecated_key_to_new_key() {
    Settings settings = new Settings(definitions);
    settings.setProperty("newKey", "value of newKey");

    assertThat(settings.getString("newKey")).isEqualTo("value of newKey");
    assertThat(settings.getString("oldKey")).isEqualTo("value of newKey");
  }

  @Test
  public void should_load_value_of_deprecated_key() {
    // it's used for example when deprecated settings are set through command-line
    Settings settings = new Settings(definitions);
    settings.setProperty("oldKey", "value of oldKey");

    assertThat(settings.getString("newKey")).isEqualTo("value of oldKey");
    assertThat(settings.getString("oldKey")).isEqualTo("value of oldKey");
  }

  @Test
  public void should_load_values_of_deprecated_key() {
    Settings settings = new Settings(definitions);
    settings.setProperty("oldKey", "a,b");

    assertThat(settings.getStringArray("newKey")).containsOnly("a", "b");
    assertThat(settings.getStringArray("oldKey")).containsOnly("a", "b");
  }

  @Test
  public void should_support_deprecated_props_with_multi_values() {
    Settings settings = new Settings(definitions);
    settings.setProperty("new_multi_values", new String[]{" A ", " B "});
    assertThat(settings.getStringArray("new_multi_values")).isEqualTo(new String[]{"A", "B"});
    assertThat(settings.getStringArray("old_multi_values")).isEqualTo(new String[]{"A", "B"});
  }
}
