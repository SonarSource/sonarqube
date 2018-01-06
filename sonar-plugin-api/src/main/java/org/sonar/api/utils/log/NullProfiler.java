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
package org.sonar.api.utils.log;

import javax.annotation.Nullable;

class NullProfiler extends Profiler {

  static final NullProfiler NULL_INSTANCE = new NullProfiler();

  private NullProfiler() {
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public Profiler start() {
    return this;
  }

  @Override
  public Profiler startTrace(String message) {
    return this;
  }

  @Override
  public Profiler startDebug(String message) {
    return this;
  }

  @Override
  public Profiler startInfo(String message) {
    return this;
  }

  @Override
  public Profiler stopTrace() {
    return this;
  }

  @Override
  public Profiler stopDebug() {
    return this;
  }

  @Override
  public Profiler stopInfo() {
    return this;
  }

  @Override
  public Profiler stopTrace(String message) {
    return this;
  }

  @Override
  public Profiler stopDebug(String message) {
    return this;
  }

  @Override
  public Profiler stopInfo(String message) {
    return this;
  }

  @Override
  public Profiler addContext(String key, @Nullable Object value) {
    // nothing to do
    return this;
  }

  @Override
  public Profiler stopInfo(boolean cacheUsed) {
    return this;
  }
}
