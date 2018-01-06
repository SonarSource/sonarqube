/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.util;

/**
 * A generator of UUID as a byte array which is made of two parts:
 * <ul>
 *   <li>a base, which is machine and time dependant and therefor will change with time</li>
 *   <li>an increment</li>
 * </ul>
 *
 * <p>
 * This generator can be used in two ways:
 * <ul>
 *   <li>either the base and the increment are changed for each UUID (with time for the base, with each call to
 *   {@link #generate()} for the increment) and {@link #generate()} should be used</li>
 *   <li>or the increment can be the only changing part (for performance boost and less concurrency) and
 *   {@link #withFixedBase()} should be used.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Warning:</strong> {@link WithFixedBase#generate(int)} can be considerably faster than {@link #generate()} but
 * is limited to generate only 2^23-1 unique values.
 * </p>
 *
 * <p>
 * Heavily inspired from Elasticsearch {@code TimeBasedUUIDGenerator}, which could be directly
 * used the day {@code UuidFactoryImpl} is moved outside module sonar-core.
 * See https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/common/TimeBasedUUIDGenerator.java
 * </p>
 */
public interface UuidGenerator {
  /**
   * Generates a UUID which base and increment are always different from any other value provided by this method.
   */
  byte[] generate();

  /**
   * Provide a new UUID generating instance which will allow generation of UUIDs which base is constant and can
   * vary according to a provided increment value (see {@link WithFixedBase#generate(int)}).
   */
  WithFixedBase withFixedBase();

  @FunctionalInterface
  interface WithFixedBase {
    /**
     * Generate a new unique universal identifier using the last 3 bytes of the specified int.
     * <p>
     * <strong>This implies that this method of a given {@link WithFixedBase} instance can only generate up to
     * 2^23-1 unique values if provided with unique int arguments.</strong>
     * </p>
     * <p>
     * This is up to the caller to ensure that unique int values are provided to a given instance's {@link #generate(int)}
     * method.
     * </p>
     * <p>
     * This method is faster than {@link UuidGenerator#generate()} due to less computation and less internal concurrency.
     * </p>
     */
    byte[] generate(int increment);
  }
}
