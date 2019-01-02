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
package org.sonar.api.utils.log;

import javax.annotation.Nullable;

/**
 *
 * @since 5.1
 */
public abstract class Profiler {

  public static Profiler create(Logger logger) {
    return new DefaultProfiler((BaseLogger) logger);
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

  public abstract Profiler startDebug(String message);

  public abstract Profiler startInfo(String message);

  /**
   * Works only if a message have been set in startXXX() methods.
   */
  public abstract Profiler stopTrace();

  public abstract Profiler stopDebug();

  public abstract Profiler stopInfo();
  
  public abstract Profiler stopInfo(boolean cacheUsed);

  public abstract Profiler stopTrace(String message);

  public abstract Profiler stopDebug(String message);

  public abstract Profiler stopInfo(String message);

  /**
   * Context information is removed if value is <code>null</code>.
   */
  public abstract Profiler addContext(String key, @Nullable Object value);

}
