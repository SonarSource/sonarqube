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
package org.sonar.api.batch.sensor.test.internal;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.test.TestCase;

import javax.annotation.CheckForNull;

public class DefaultTestCase implements TestCase {

  private final InputFile testFile;
  private final String name;
  private final Long duration;
  private final Status status;
  private final String message;
  private final Type type;
  private final String stackTrace;

  public DefaultTestCase(InputFile testFile, String name, Long duration, Status status, String message, Type type, String stackTrace) {
    this.testFile = testFile;
    this.name = name;
    this.duration = duration;
    this.status = status;
    this.message = message;
    this.type = type;
    this.stackTrace = stackTrace;
  }

  public InputFile testFile() {
    return testFile;
  }

  @CheckForNull
  @Override
  public Long durationInMs() {
    return duration;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public Status status() {
    return status;
  }

  @Override
  public String name() {
    return name;
  }

  @CheckForNull
  @Override
  public String message() {
    return message;
  }

  @CheckForNull
  @Override
  public String stackTrace() {
    return stackTrace;
  }

  // Just for unit tests
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    DefaultTestCase rhs = (DefaultTestCase) obj;
    return new EqualsBuilder()
      .append(testFile, rhs.testFile)
      .append(name, rhs.name)
      .append(duration, rhs.duration)
      .append(status, rhs.status)
      .append(message, rhs.message)
      .append(type, rhs.type)
      .append(stackTrace, rhs.stackTrace)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(13, 43)
      .append(testFile)
      .append(name)
      .append(duration)
      .append(status)
      .append(message)
      .append(type)
      .append(stackTrace)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("file", testFile)
      .append("name", name)
      .append("duration", duration)
      .append("status", status)
      .append("message", message)
      .append("type", type)
      .append("stackTrace", stackTrace)
      .toString();
  }
}
