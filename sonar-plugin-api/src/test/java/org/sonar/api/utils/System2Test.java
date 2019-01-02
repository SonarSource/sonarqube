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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class System2Test {
  @Test
  public void testNow() {
    long start = System.currentTimeMillis();
    long now = System2.INSTANCE.now();
    long end = System.currentTimeMillis();
    assertThat(now).isGreaterThanOrEqualTo(start).isLessThanOrEqualTo(end);
  }

  @Test
  public void testProperties() {
    Properties expected = System.getProperties();
    assertThat(System2.INSTANCE.properties()).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testProperty() {
    String expected = System.getProperty("java.version");
    assertThat(System2.INSTANCE.property("java.version")).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testSetProperty() {
    String key = "System2Test.testSetProperty";
    String value = randomUUID().toString();
    System2.INSTANCE.setProperty(key, value);
    assertThat(System2.INSTANCE.property(key)).isEqualTo(value);
  }

  @Test
  public void testEnvVariables() {
    Map<String, String> expected = System.getenv();
    assertThat(System2.INSTANCE.envVariables()).isNotNull().isEqualTo(expected);
  }

  @Test
  public void testEnvVariable() {
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
  public void testIsOsWindows() {
    assertThat(System2.INSTANCE.isOsWindows()).isEqualTo(SystemUtils.IS_OS_WINDOWS);
  }

  @Test
  public void isJavaAtLeast17_always_returns_true() {
    assertThat(System2.INSTANCE.isJavaAtLeast17()).isTrue();
  }

  @Test
  public void testPrintln() {
    // well, how to assert that ? Adding a System3 dependency to System2 ? :-)
    System2.INSTANCE.println("foo");
  }

  @Test
  public void testGetResource() {
    String name = "META-INF/MANIFEST.MF";
    assertThat(System2.INSTANCE.getResource(name)).isEqualTo(getClass().getResource(name));
  }

  @Test
  public void close() {
    class MyCloseable implements Closeable {
      boolean isClosed = false;

      @Override
      public void close() throws IOException {
        isClosed = true;
      }
    }

    MyCloseable closeable = new MyCloseable();
    System2.INSTANCE.close(closeable);
    assertThat(closeable.isClosed).isTrue();
  }

  @Test
  public void close_throws_exception_on_error() {
    Closeable closeable = new Closeable() {
      @Override
      public void close() throws IOException {
        throw new IOException("expected");
      }
    };
    try {
      System2.INSTANCE.close(closeable);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getCause().getMessage()).isEqualTo("expected");
    }
  }

  @Test
  public void getDefaultTimezone() {
    assertThat(System2.INSTANCE.getDefaultTimeZone().getID()).isEqualTo(TimeZone.getDefault().getID());

  }
}
