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

import java.net.URL;
import java.time.Clock;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.SystemUtils;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;

/**
 * Proxy over {@link java.lang.System}. It aims to improve testability of classes
 * that interact with low-level system methods, for example :
 * <br>
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
 * <br>
 * Note that the name System2 was chosen to not conflict with {@link java.lang.System}.
 * <br>
 * An instance is available in IoC container since 4.3.
 * Since 6.4 you can also inject {@link Clock} instead of {@link System2} if you are only interested by date/time operations
 *
 * @since 4.2
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class System2 {

  public static final System2 INSTANCE = new System2();

  /**
   * Shortcut for {@link System#currentTimeMillis()}
   * Since 6.4 you can also inject {@link Clock} instead of {@link System2}
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
   * Shortcut for {@code System{@link #setProperty(String, String)}}
   *
   * @since 6.4
   */
  public System2 setProperty(String key, String value) {
    System.setProperty(key, value);
    return this;
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
   * True if this is Mac system.
   */
  public boolean isOsMac() {
    return SystemUtils.IS_OS_MAC;
  }

  /**
   * True if Java 7 or Java 8 runtime environment
   *
   * @since 4.3
   * @deprecated in 6.4. Java 8+ is required, so this method always returns {@code true}.
   */
  @Deprecated
  public boolean isJavaAtLeast17() {
    return true;
  }

  public void println(String obj) {
    System.out.print(obj);
  }

  /**
   * @return the JVM's default time zone
   * @since 5.1
   */
  public TimeZone getDefaultTimeZone() {
    return TimeZone.getDefault();
  }

  /**
   * @see Class#getResource(String)
   * @since 5.5
   */
  public URL getResource(String name) {
    return getClass().getResource(name);
  }

  /**
   * Closes the object and throws an {@link java.lang.IllegalStateException} on error.
   *
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
