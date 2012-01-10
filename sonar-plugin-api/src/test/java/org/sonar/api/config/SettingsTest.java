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

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.Property;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsTest {

  private PropertyDefinitions definitions;

  @Before
  public void initDefinitions() {
    definitions = new PropertyDefinitions();
    definitions.addProperty(newProperty("hello", "world"));
    definitions.addProperty(newProperty("date", "2010-05-18"));
    definitions.addProperty(newProperty("boolean", "true"));
    definitions.addProperty(newProperty("falseboolean", "false"));
    definitions.addProperty(newProperty("integer", "12345"));
    definitions.addProperty(newProperty("array", "one,two,three"));
  }

  private Property newProperty(String key, String defaultValue) {
    Property prop = mock(Property.class);
    when(prop.key()).thenReturn(key);
    when(prop.defaultValue()).thenReturn(defaultValue);
    return prop;
  }

  @Test
  public void defaultValuesShouldBeLoadedFromDefinitions() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDefaultValue("hello"), is("world"));
  }

  @Test
  public void testGetDefaultValue() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDefaultValue("unknown"), nullValue());
  }

  @Test
  public void testGetString() {
    Settings settings = new Settings(definitions);
    settings.setProperty("hello", "Russia");
    assertThat(settings.getString("hello"), is("Russia"));
  }

  @Test
  public void testGetDate() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDate("date").getDate(), is(18));
    assertThat(settings.getDate("date").getMonth(), is(4));
  }

  @Test
  public void testGetDateNotFound() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getDate("unknown"), CoreMatchers.<Object>nullValue());
  }

  @Test
  public void testGetArray() {
    Settings settings = new Settings(definitions);
    String[] array = settings.getStringArray("array");
    assertThat(array.length, is(3));
    assertThat(array[0], is("one"));
    assertThat(array[1], is("two"));
    assertThat(array[2], is("three"));
  }

  @Test
  public void shouldTrimArray() {
    Settings settings = new Settings();
    settings.setProperty("foo", "  one,  two, three  ");
    String[] array = settings.getStringArray("foo");
    assertThat(array.length, is(3));
    assertThat(array[0], is("one"));
    assertThat(array[1], is("two"));
    assertThat(array[2], is("three"));
  }

  @Test
  public void shouldKeepEmptyValuesWhenSplitting() {
    Settings settings = new Settings();
    settings.setProperty("foo", "  one,  , two");
    String[] array = settings.getStringArray("foo");
    assertThat(array.length, is(3));
    assertThat(array[0], is("one"));
    assertThat(array[1], is(""));
    assertThat(array[2], is("two"));
  }

  @Test
  public void testDefaultValueOfGetString() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getString("hello"), is("world"));
  }

  @Test
  public void testGetBoolean() {
    Settings settings = new Settings(definitions);
    assertThat(settings.getBoolean("boolean"), is(true));
    assertThat(settings.getBoolean("falseboolean"), is(false));
    assertThat(settings.getBoolean("unknown"), is(false));
    assertThat(settings.getBoolean("hello"), is(false));
  }

  @Test
  public void shouldCreateByIntrospectingComponent() {
    Settings settings = Settings.createForComponent(MyComponent.class);

    // property definition has been loaded, ie for default value
    assertThat(settings.getDefaultValue("foo"), is("bar"));
  }

  @Property(key = "foo", name = "Foo", defaultValue = "bar")
  public static class MyComponent {

  }
}
