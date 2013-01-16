/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.api.tests;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Test {

  private String name;
  private String stackTrace;
  private String message;
  private long durationMilliseconds;
  private String status;

  public Test(String name) {
    this.name = name;
  }

  public long getDurationMilliseconds() {
    return durationMilliseconds;
  }

  public Test setDurationMilliseconds(long durationMilliseconds) {
    this.durationMilliseconds = durationMilliseconds;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Test setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public Test setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public Test setMessage(String message) {
    this.message = message;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Test test = (Test) o;

    if (!name.equals(test.name)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("durationMilliseconds", durationMilliseconds)
        .append("status", status)
        .append("message", message)
        .append("stackTrace", stackTrace)
        .toString();
  }
}
