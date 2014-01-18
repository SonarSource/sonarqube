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
package org.sonar.api.utils;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class System2Test {
  @Test
  public void testNow() throws Exception {
    long start = System.currentTimeMillis();
    long now = System2.INSTANCE.now();
    assertThat(now-start).isGreaterThanOrEqualTo(0).isLessThan(3);
  }

  @Test
  public void testProperties() throws Exception {
    Properties expected = System.getProperties();
    assertThat(System2.INSTANCE.properties()).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testProperty() throws Exception {
    String expected = System.getProperty("java.version");
    assertThat(System2.INSTANCE.property("java.version")).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testEnvVariables() throws Exception {
    Map<String,String> expected = System.getenv();
    assertThat(System2.INSTANCE.envVariables()).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testEnvVariable() throws Exception {
    // assume that there's at least one env variable
    if (System.getenv().isEmpty()) {
      fail("Test can't succeed because there are no env variables. How is it possible ?");
    }
    String key = System.getenv().keySet().iterator().next();
    String expected = System.getenv(key);
    assertThat(System2.INSTANCE.envVariable(key)).isNotNull().isEqualTo(expected);
    assertThat(System2.INSTANCE.envVariable("UNKNOWN_VAR")).isNull();
  }

  @Test
  public void testIsOsWindows() throws Exception {
    assertThat(System2.INSTANCE.isOsWindows()).isEqualTo(SystemUtils.IS_OS_WINDOWS);
  }

  @Test
  public void testPrintln() throws Exception {
    // well, how to assert that ? Adding a System3 dependency to System2 ? :-)
    System2.INSTANCE.println("foo");
  }
}
