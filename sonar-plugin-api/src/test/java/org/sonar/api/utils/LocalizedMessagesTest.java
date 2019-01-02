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

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalizedMessagesTest {
  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  @BeforeClass
  public static void beforeAll() {
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterClass
  public static void afterAll() {
    Locale.setDefault(DEFAULT_LOCALE);
  }

  @Test
  public void mergeBundles() {
    LocalizedMessages messages = new LocalizedMessages(Locale.ENGLISH, "Test", "PluginFoo");

    assertThat(messages.getString("test.one")).isEqualTo("One");
    assertThat(messages.getString("test.two")).isEqualTo("Two");
    assertThat(messages.getString("foo.hello")).isEqualTo("Hello");
  }

  @Test
  public void mergeBundlesByLocale() {
    LocalizedMessages messages = new LocalizedMessages(Locale.FRENCH, "Test", "PluginFoo");

    assertThat(messages.getString("test.one")).isEqualTo("Un");
    assertThat(messages.getString("test.two")).isEqualTo("Deux");
    assertThat(messages.getString("foo.hello")).isEqualTo("Hello");// not in french, use the default locale
  }

  @Test
  public void useDefaultWhenMissingLocale() {
    LocalizedMessages messages = new LocalizedMessages(Locale.JAPANESE, "Test", "PluginFoo");

    assertThat(messages.getString("test.one")).isEqualTo("One");
    assertThat(messages.getString("foo.hello")).isEqualTo("Hello");
  }

  @Test(expected = MissingResourceException.class)
  public void failIfMissingKey() {
    LocalizedMessages messages = new LocalizedMessages(Locale.FRENCH, "Test", "PluginFoo");
    messages.getString("unknown");
  }

  @Test
  public void format() {
    LocalizedMessages messages = new LocalizedMessages(Locale.ENGLISH, "Test", "PluginFoo");
    assertThat(messages.format("test.one")).isEqualTo("One");
  }

  @Test
  public void formatNeverFails() {
    LocalizedMessages messages = new LocalizedMessages(Locale.ENGLISH, "Test", "PluginFoo");
    assertThat(messages.format("unknown")).isEqualTo("unknown");
  }

  @Test
  public void formatParameters() {
    LocalizedMessages messages = new LocalizedMessages(Locale.ENGLISH, "Test", "PluginFoo");
    assertThat(messages.format("with.string.params", "inspection", "rock")).isEqualTo("Continuous inspection will rock !");
    assertThat(messages.format("with.string.params", "rock", "inspection")).isEqualTo("Continuous rock will inspection !");
  }

  @Test
  public void getKeys() {
    LocalizedMessages messages = new LocalizedMessages(Locale.ENGLISH, "Test", "PluginFoo");
    assertThat(toList(messages.getKeys())).contains("test.one", "test.two", "foo.hello");

    LocalizedMessages spanishMessages = new LocalizedMessages(new Locale("es"), "Test", "PluginFoo");
    assertThat(toList(spanishMessages.getKeys())).contains("test.one", "only.in.spanish");
  }

  private List<String> toList(Enumeration<String> enumeration) {
    return Lists.newArrayList(Iterators.forEnumeration(enumeration));
  }
}
