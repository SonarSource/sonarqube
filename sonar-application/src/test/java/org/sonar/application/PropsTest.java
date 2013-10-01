/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    Props props = new Props(p);

    assertThat(props.intOf("foo")).isEqualTo(33);
    assertThat(props.intOf("foo", 44)).isEqualTo(33);
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
  public void load_file_and_system_properties() throws Exception {
    Env env = mock(Env.class);
    File propsFile = new File(getClass().getResource("/org/sonar/application/PropsTest/sonar.properties").toURI());
    when(env.file("conf/sonar.properties")).thenReturn(propsFile);

    Props props = Props.create(env);

    assertThat(props.of("foo")).isEqualTo("bar");
    assertThat(props.of("java.version")).isNotNull();

    // system properties override file properties
    assertThat(props.of("java.io.tmpdir")).isNotEmpty().isNotEqualTo("/should/be/overridden");
  }

  @Test
  public void fail_if_file_does_not_exist() throws Exception {
    Env env = mock(Env.class);
    when(env.file("conf/sonar.properties")).thenReturn(new File("target/not_exist/sonar.properties"));

    try {
      Props.create(env);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("File does not exist or can't be open: target/not_exist/sonar.properties");
    }
  }
}
