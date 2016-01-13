/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PropsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void value() {
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    Props props = new Props(p);

    assertThat(props.value("foo")).isEqualTo("bar");
    assertThat(props.value("foo", "default value")).isEqualTo("bar");
    assertThat(props.value("unknown")).isNull();
    assertThat(props.value("unknown", "default value")).isEqualTo("default value");

    assertThat(props.nonNullValue("foo")).isEqualTo("bar");
    try {
      props.nonNullValue("other");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Missing property: other");
    }
  }

  @Test
  public void valueAsInt() {
    Properties p = new Properties();
    p.setProperty("foo", "33");
    p.setProperty("blank", "");
    Props props = new Props(p);

    assertThat(props.valueAsInt("foo")).isEqualTo(33);
    assertThat(props.valueAsInt("foo", 44)).isEqualTo(33);
    assertThat(props.valueAsInt("blank")).isNull();
    assertThat(props.valueAsInt("blank", 55)).isEqualTo(55);
    assertThat(props.valueAsInt("unknown")).isNull();
    assertThat(props.valueAsInt("unknown", 44)).isEqualTo(44);
  }

  @Test
  public void valueAsInt_not_integer() {
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    Props props = new Props(p);

    try {
      props.valueAsInt("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Value of property foo is not an integer: bar");
    }
  }

  @Test
  public void booleanOf() {
    Properties p = new Properties();
    p.setProperty("foo", "True");
    p.setProperty("bar", "false");
    Props props = new Props(p);

    assertThat(props.valueAsBoolean("foo")).isTrue();
    assertThat(props.valueAsBoolean("bar")).isFalse();
    assertThat(props.valueAsBoolean("unknown")).isFalse();
  }

  @Test
  public void booleanOf_default_value() {
    Properties p = new Properties();
    p.setProperty("foo", "true");
    p.setProperty("bar", "false");
    Props props = new Props(p);

    assertThat(props.valueAsBoolean("unset", false)).isFalse();
    assertThat(props.valueAsBoolean("unset", true)).isTrue();
    assertThat(props.valueAsBoolean("foo", false)).isTrue();
    assertThat(props.valueAsBoolean("bar", true)).isFalse();
  }

  @Test
  public void setDefault() {
    Properties p = new Properties();
    p.setProperty("foo", "foo_value");
    Props props = new Props(p);
    props.setDefault("foo", "foo_def");
    props.setDefault("bar", "bar_def");

    assertThat(props.value("foo")).isEqualTo("foo_value");
    assertThat(props.value("bar")).isEqualTo("bar_def");
    assertThat(props.value("other")).isNull();
  }

  @Test
  public void set() {
    Properties p = new Properties();
    p.setProperty("foo", "old_foo");
    Props props = new Props(p);
    props.set("foo", "new_foo");
    props.set("bar", "new_bar");

    assertThat(props.value("foo")).isEqualTo("new_foo");
    assertThat(props.value("bar")).isEqualTo("new_bar");
  }

  @Test
  public void raw_properties() {
    Properties p = new Properties();
    p.setProperty("encrypted_prop", "{aes}abcde");
    p.setProperty("clear_prop", "foo");
    Props props = new Props(p);

    assertThat(props.rawProperties()).hasSize(2);
    // do not decrypt
    assertThat(props.rawProperties().get("encrypted_prop")).isEqualTo("{aes}abcde");
    assertThat(props.rawProperties().get("clear_prop")).isEqualTo("foo");
  }

  @Test
  public void nonNullValueAsFile() throws IOException {
    File file = temp.newFile();
    Props props = new Props(new Properties());
    props.set("path", file.getAbsolutePath());

    assertThat(props.nonNullValueAsFile("path")).isEqualTo(file);

    try {
      props.nonNullValueAsFile("other_path");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Property other_path is not set");
    }
  }
}
