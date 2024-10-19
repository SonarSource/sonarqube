/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.impl.utils;

import java.util.TimeZone;
import org.sonar.api.utils.System2;

public class TestSystem2 extends System2 {

  private long now = 0L;
  private TimeZone defaultTimeZone = TimeZone.getTimeZone("UTC");

  public TestSystem2 setNow(long l) {
    this.now = l;
    return this;
  }

  public TestSystem2 tick() {
    throwExceptionIfNowLesserOrEqualZero();
    this.now = this.now + 1;
    return this;
  }

  @Override
  public long now() {
    throwExceptionIfNowLesserOrEqualZero();
    return now;
  }

  private void throwExceptionIfNowLesserOrEqualZero() {
    if (now <= 0L) {
      throw new IllegalStateException("Method setNow() was not called by test");
    }
  }

  public TestSystem2 setDefaultTimeZone(TimeZone defaultTimeZone) {
    this.defaultTimeZone = defaultTimeZone;
    return this;
  }

  @Override
  public TimeZone getDefaultTimeZone() {
    return defaultTimeZone;
  }
}
