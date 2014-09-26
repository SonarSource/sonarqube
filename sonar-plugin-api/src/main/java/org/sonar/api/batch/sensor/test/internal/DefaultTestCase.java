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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.test.TestCase;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class DefaultTestCase implements TestCase {

  private final SensorStorage storage;
  private InputFile testFile;
  private String name;
  private Long duration;
  private TestCase.Status status = Status.OK;
  private String message;
  private TestCase.Type type = Type.UNIT;
  private String stackTrace;

  public DefaultTestCase(SensorStorage storage) {
    this.storage = storage;
  }

  @Override
  public DefaultTestCase inTestFile(InputFile testFile) {
    Preconditions.checkNotNull(testFile, "TestFile cannot be null");
    Preconditions.checkArgument(testFile.type() == InputFile.Type.TEST, "Should be a test file: " + testFile);
    this.testFile = testFile;
    return this;
  }

  @Override
  public DefaultTestCase name(String name) {
    Preconditions.checkArgument(StringUtils.isNotBlank(name), "Test name is mandatory and should not be blank");
    this.name = name;
    return this;
  }

  @Override
  public DefaultTestCase durationInMs(long duration) {
    Preconditions.checkArgument(duration >= 0, "Test duration must be positive (got: " + duration + ")");
    this.duration = duration;
    return this;
  }

  @Override
  public DefaultTestCase status(TestCase.Status status) {
    Preconditions.checkNotNull(status);
    this.status = status;
    return this;
  }

  @Override
  public DefaultTestCase message(@Nullable String message) {
    this.message = message;
    return this;
  }

  @Override
  public DefaultTestCase ofType(TestCase.Type type) {
    Preconditions.checkNotNull(type);
    this.type = type;
    return this;
  }

  @Override
  public DefaultTestCase stackTrace(@Nullable String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
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

  @Override
  public void save() {
    Preconditions.checkNotNull(this.storage, "No persister on this object");
    Preconditions.checkNotNull(testFile, "TestFile is mandatory");
    Preconditions.checkNotNull(name, "TestFile is mandatory");
    storage.store(this);
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
