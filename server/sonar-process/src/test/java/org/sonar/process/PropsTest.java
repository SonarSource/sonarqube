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
package org.sonar.process;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class PropsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void constructor_trims_key_and_values_from_Properties_argument(String blankBefore, String blankAfter) {
    Properties properties = new Properties();
    String key = RandomStringUtils.randomAlphanumeric(3);
    String value = RandomStringUtils.randomAlphanumeric(3);
    properties.put(blankBefore + key + blankAfter, blankBefore + value + blankAfter);

    Props underTest = new Props(properties);

    if (!blankBefore.isEmpty() || !blankAfter.isEmpty()) {
      assertThat(underTest.contains(blankBefore + key + blankAfter)).isFalse();
    }
    assertThat(underTest.value(key)).isEqualTo(value);
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void value(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty(blankBefore + "foo" + blankAfter, blankBefore + "bar" + blankAfter);
    p.setProperty("blank", blankBefore + blankAfter);
    Props props = new Props(p);

    assertThat(props.value("foo")).isEqualTo("bar");
    assertThat(props.value("foo", "default value")).isEqualTo("bar");
    assertThat(props.value("blank")).isEmpty();
    assertThat(props.value("blank", "default value")).isEmpty();
    assertThat(props.value("unknown")).isNull();
    assertThat(props.value("unknown", "default value")).isEqualTo("default value");
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void nonNullValue(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty("foo", blankBefore + "bar" + blankAfter);
    Props props = new Props(p);

    assertThat(props.nonNullValue("foo")).isEqualTo("bar");
  }

  @Test
  public void nonNullValue_throws_IAE_on_non_existing_key() {
    Props props = new Props(new Properties());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property: other");

    props.nonNullValue("other");
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void nonNullValue_return_empty_string_IAE_on_existing_key_with_blank_value(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty("blank", blankBefore + blankAfter);
    Props props = new Props(p);

    assertThat(props.nonNullValue("blank")).isEmpty();
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void valueAsInt(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty("foo", blankBefore + "33" + blankAfter);
    p.setProperty("blank", blankBefore + blankAfter);
    Props props = new Props(p);

    assertThat(props.valueAsInt("foo")).isEqualTo(33);
    assertThat(props.valueAsInt("foo", 44)).isEqualTo(33);
    assertThat(props.valueAsInt("blank")).isNull();
    assertThat(props.valueAsInt("blank", 55)).isEqualTo(55);
    assertThat(props.valueAsInt("unknown")).isNull();
    assertThat(props.valueAsInt("unknown", 44)).isEqualTo(44);
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void valueAsInt_not_integer(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty("foo", blankBefore + "bar" + blankAfter);
    Props props = new Props(p);

    try {
      props.valueAsInt("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Value of property foo is not an integer: bar");
    }
  }

  @Test
  @UseDataProvider("beforeAndAfterBlanks")
  public void booleanOf(String blankBefore, String blankAfter) {
    Properties p = new Properties();
    p.setProperty("foo", blankBefore + "True" + blankAfter);
    p.setProperty("bar", blankBefore + "false" + blankAfter);
    Props props = new Props(p);

    assertThat(props.valueAsBoolean("foo")).isTrue();
    assertThat(props.valueAsBoolean("bar")).isFalse();
    assertThat(props.valueAsBoolean("foo", false)).isTrue();
    assertThat(props.valueAsBoolean("bar", true)).isFalse();
    assertThat(props.valueAsBoolean("unknown")).isFalse();
    assertThat(props.valueAsBoolean("unset", false)).isFalse();
    assertThat(props.valueAsBoolean("unset", true)).isTrue();
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

  @DataProvider
  public static Object[][] beforeAndAfterBlanks() {
    return new Object[][] {
      {"", ""},
      {" ", ""},
      {"", " "},
      {" ", " "},
    };
  }
}
