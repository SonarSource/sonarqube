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
package org.sonar.core.util.logs;

import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;

/**
 *
 * @since 5.1
 */
public abstract class Profiler {

  public static Profiler create(Logger logger) {
    return new DefaultProfiler(logger);
  }

  public static Profiler createIfTrace(Logger logger) {
    if (logger.isTraceEnabled()) {
      return create(logger);
    }
    return NullProfiler.NULL_INSTANCE;
  }

  public static Profiler createIfDebug(Logger logger) {
    if (logger.isDebugEnabled()) {
      return create(logger);
    }
    return NullProfiler.NULL_INSTANCE;
  }

  public abstract boolean isDebugEnabled();

  public abstract boolean isTraceEnabled();

  public abstract Profiler start();

  public abstract Profiler startTrace(String message);

  public abstract Profiler startTrace(String message, Object... args);

  public abstract Profiler startDebug(String message);

  public abstract Profiler startDebug(String message, Object... args);

  public abstract Profiler startInfo(String message);

  public abstract Profiler startInfo(String message, Object... args);

  /**
   * Works only if a message have been set in startXXX() methods.
   */
  public abstract long stopTrace();

  public abstract long stopDebug();

  public abstract long stopInfo();

  public abstract long stopTrace(String message);

  public abstract long stopTrace(String message, Object... args);

  public abstract long stopDebug(String message);

  public abstract long stopDebug(String message, Object... args);

  public abstract long stopInfo(String message);

  public abstract long stopInfo(String message, Object... args);

  public abstract long stopError(String message, Object... args);

  /**
   * Context information is removed if value is <code>null</code>.
   */
  public abstract Profiler addContext(String key, @Nullable Object value);

  public abstract boolean hasContext(String key);

  /**
   * Defines whether time is added to stop messages before or after context (if any).
   * <p>{@code flag} is {@code false} by default.</p>
   */
  public abstract Profiler logTimeLast(boolean flag);
}
