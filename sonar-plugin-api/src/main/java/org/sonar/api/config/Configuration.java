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
package org.sonar.api.config;

import java.util.Optional;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Component to get effective configuration. Values of properties depend on the runtime environment:
 * <ul>
 *   <li>immutable project configuration in scanner.</li>
 *   <li>global configuration in web server. It does not allow to get the settings overridden on projects.</li>
 *   <li>project configuration in Compute Engine.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * public class MyExtension {
 *
 *   private final Configuration config;
 *
 *   public MyExtension(Configuration config) {
 *     this.config = config;
 *   }
 *   public void doSomething() {
 *     String fooValue = config.get("sonar.foo").orElse(null);
 *     // ..
 *   }
 * }
 * </pre>
 *
 * <h3>Scanner example</h3>
 * Scanner sensor can get the reference on Configuration directly through SensorContext,
 * without injecting the component into constructor.
 *
 * <pre>
 * public class MySensor implements Sensor {
 *   {@literal @}Override
 *   public void execute(SensorContext context) {
 *     String fooValue = context.config().get("sonar.foo").orElse(null);
 *     // ..
 *   }
 * }
 * </pre>
 *
 * <p>
 * For testing, and only for testing, the in-memory implementation MapSettings can be used.
 * <pre>
 * {@literal @}Test
 * public void my_test() {
 *   MapSettings settings = new MapSettings();
 *   settings.setProperty("foo", "bar");
 *   MyExtension underTest = new MyExtension(settings.asConfig());
 *   // ...
 * }
 * </pre>
 *
 * @see PropertyDefinition
 * @since 6.5
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
@SonarLintSide
public interface Configuration {

  /**
   * The effective value of the specified property. Can return {@code Optional#empty()} if the property is not set and has no defined default value.
   * <p>
   * If the property is encrypted with a secret key, then the returned value is decrypted.
   * </p>
   *
   * @throws IllegalStateException if value is encrypted but fails to be decrypted.
   */
  Optional<String> get(String key);

  /**
   * @return {@code true} if the property has a non-default value, else {@code false}.
   */
  boolean hasKey(String key);

  /**
   * Used to read multi-valued properties. 
   * <p>
   * See {@link PropertyDefinition.Builder#multiValues(boolean)}
   * Multi-valued properties coming from scanner are parsed as CSV lines (ie comma separator and optional double quotes to escape values).
   * Non quoted values are trimmed and empty fields are ignored.
   * <br>
   * Examples :
   * <ul>
   * <li>"one,two,three " -&gt; ["one", "two", "three"]</li>
   * <li>"  one, two, three " -&gt; ["one", "two", "three"]</li>
   * <li>"one, three" -&gt; ["one", "three"]</li>
   * <li>"one,"", three" -&gt; ["one", "", "three"]</li>
   * <li>"one,  "  " , three" -&gt; ["one", "  ", "three"]</li>
   * <li>"one,\"two,three\",\" four \"" -&gt; ["one", "two,three", " four "]</li>
   * </ul>
   */
  String[] getStringArray(String key);

  /**
   * Effective value as boolean. It is {@code empty} if {@link #get(String)} is empty or if it
   * does not return {@code "true"}, even if it's not a boolean representation.
   * @return {@code true} if the effective value is {@code "true"}, {@code false} for any other non empty value. 
   * If the property does not have value nor default value, then {@code empty} is returned.
   */
  default Optional<Boolean> getBoolean(String key) {
    return get(key).map(String::trim).map(Boolean::parseBoolean);
  }

  /**
   * Effective value as {@code int}.
   * @return the value as {@code int}. If the property does not have value nor default value, then {@code empty} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable integer
   */
  default Optional<Integer> getInt(String key) {
    try {
      return get(key).map(String::trim).map(Integer::parseInt);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(String.format("The property '%s' is not an int value: %s", key, e.getMessage()));
    }
  }

  /**
   * Effective value as {@code long}.
   * @return the value as {@code long}. If the property does not have value nor default value, then {@code empty} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable {@code long}
   */
  default Optional<Long> getLong(String key) {
    try {
      return get(key).map(String::trim).map(Long::parseLong);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(String.format("The property '%s' is not an long value: %s", key, e.getMessage()));
    }
  }

  /**
   * Effective value as {@code Float}.
   * @return the value as {@code Float}. If the property does not have value nor default value, then {@code empty} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable number
   */
  default Optional<Float> getFloat(String key) {
    try {
      return get(key).map(String::trim).map(Float::valueOf);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(String.format("The property '%s' is not an float value: %s", key, e.getMessage()));
    }
  }

  /**
   * Effective value as {@code Double}.
   * @return the value as {@code Double}. If the property does not have value nor default value, then {@code empty} is returned.
   * @throws NumberFormatException if value is not empty and is not a parsable number
   */
  default Optional<Double> getDouble(String key) {
    try {
      return get(key).map(String::trim).map(Double::valueOf);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(String.format("The property '%s' is not an double value: %s", key, e.getMessage()));
    }
  }

}
