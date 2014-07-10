/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import org.junit.Test;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class PropsTest {

  @Test
  public void of() throws Exception {
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    Props props = new Props(p);

    assertThat(props.of("foo")).isEqualTo("bar");
    assertThat(props.of("foo", "default value")).isEqualTo("bar");
    assertThat(props.of("unknown")).isNull();
    assertThat(props.of("unknown", "default value")).isEqualTo("default value");
  }

  @Test
  public void intOf() throws Exception {
    Properties p = new Properties();
    p.setProperty("foo", "33");
    p.setProperty("blank", "");
    Props props = new Props(p);

    assertThat(props.intOf("foo")).isEqualTo(33);
    assertThat(props.intOf("foo", 44)).isEqualTo(33);
    assertThat(props.intOf("blank")).isNull();
    assertThat(props.intOf("blank", 55)).isEqualTo(55);
    assertThat(props.intOf("unknown")).isNull();
    assertThat(props.intOf("unknown", 44)).isEqualTo(44);
  }

  @Test
  public void intOf_not_integer() throws Exception {
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    Props props = new Props(p);

    try {
      props.intOf("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Value of property foo is not an integer: bar");
    }
  }

  @Test
  public void booleanOf() throws Exception {
    Properties p = new Properties();
    p.setProperty("foo", "True");
    p.setProperty("bar", "false");
    Props props = new Props(p);

    assertThat(props.booleanOf("foo")).isTrue();
    assertThat(props.booleanOf("bar")).isFalse();
    assertThat(props.booleanOf("unknown")).isFalse();
  }

  @Test
  public void booleanOf_default_value() throws Exception {
    Properties p = new Properties();
    p.setProperty("foo", "true");
    p.setProperty("bar", "false");
    Props props = new Props(p);

    assertThat(props.booleanOf("unset", false)).isFalse();
    assertThat(props.booleanOf("unset", true)).isTrue();
    assertThat(props.booleanOf("foo", false)).isTrue();
    assertThat(props.booleanOf("bar", true)).isFalse();
  }
}
