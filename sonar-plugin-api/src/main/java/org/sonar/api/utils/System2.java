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
package org.sonar.api.utils;

import org.apache.commons.lang.SystemUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Proxy over {@link java.lang.System}. It aims to improve testability of classes
 * that interact with low-level system methods, for example :
 * <p/>
 * <pre>
 * public class MyClass {
 *   private final System2 system;
 *
 *   public MyClass(System2 s) {
 *     this.system = s;
 *   }
 *
 *   public long xxx() {
 *     return system.now();
 *   }
 * }
 *
 * {@literal @}Test
 * public void should_return_xxx() {
 *   // using Mockito
 *   System2 system = mock(System2.class);
 *   long now = 123456789L;
 *   doReturn(now).when(system).now();
 *   assertThat(new MyClass(system).xxx()).isEqualTo(now);
 * }
 * </pre>
 * <p/>
 * Note that the name System2 was chosen to not conflict with {@link java.lang.System}.
 * <p/>
 * An instance is available in IoC container since 4.3.
 *
 * @since 4.2
 */
@BatchSide
@ServerSide
public class System2 {

  public static final System2 INSTANCE = new System2();

  /**
   * Shortcut for {@link System#currentTimeMillis()}
   */
  public long now() {
    return System.currentTimeMillis();
  }

  /**
   * Shortcut for {@link System#getProperties()}
   */
  public Properties properties() {
    return System.getProperties();
  }

  /**
   * Shortcut for {@link System#getProperty(String)}
   */
  @CheckForNull
  public String property(String key) {
    return System.getProperty(key);
  }

  /**
   * Shortcut for {@link System#getenv()}
   */
  public Map<String, String> envVariables() {
    return System.getenv();
  }

  /**
   * Shortcut for {@link System#getenv(String)}
   */
  @CheckForNull
  public String envVariable(String key) {
    return System.getenv(key);
  }

  /**
   * True if this is MS Windows.
   */
  public boolean isOsWindows() {
    return SystemUtils.IS_OS_WINDOWS;
  }

  /**
   * True if Java 7 or Java 8 runtime environment
   * @since 4.3
   */
  public boolean isJavaAtLeast17() {
    return SystemUtils.isJavaVersionAtLeast(1.7f);
  }

  public void println(String obj) {
    System.out.print(obj);
  }

  /**
   * @deprecated in 5.2. Please use {@link #now()}
   */
  @Deprecated
  public Date newDate() {
    return new Date();
  }

  /**
   * @since 5.1
   * @return the JVM's default time zone
   */
  public TimeZone getDefaultTimeZone() {
    return TimeZone.getDefault();
  }

  /**
   * Closes the object and throws an {@link java.lang.IllegalStateException} on error.
   * @since 5.1
   */
  public void close(AutoCloseable closeable) {
    try {
      closeable.close();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to close " + closeable, e);
    }
  }
}
