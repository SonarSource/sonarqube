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

/**
 * Log interceptor that does nothing, so production-ready!
 */
class NullInterceptor implements LogInterceptor {

  static final NullInterceptor NULL_INSTANCE = new NullInterceptor();

  private NullInterceptor() {
  }

  @Override
  public void log(LoggerLevel level, String msg) {
    // nothing
  }

  @Override
  public void log(LoggerLevel level, String msg, Object arg) {
    // nothing
  }

  @Override
  public void log(LoggerLevel level, String msg, Object arg1, Object arg2) {
    // nothing
  }

  @Override
  public void log(LoggerLevel level, String msg, Object... args) {
    // nothing
  }

  @Override
  public void log(LoggerLevel level, String msg, Throwable thrown) {
    // nothing
  }
}
