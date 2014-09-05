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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.TestCase.Status;
import org.sonar.api.batch.sensor.test.TestCase.Type;
import org.sonar.api.batch.sensor.test.TestCaseBuilder;

import javax.annotation.Nullable;

public class DefaultTestCaseBuilder implements TestCaseBuilder {

  private final InputFile testFile;
  private final String name;
  private Long duration;
  private TestCase.Status status = Status.OK;
  private String message;
  private TestCase.Type type = Type.UNIT;
  private String stackTrace;

  public DefaultTestCaseBuilder(InputFile testFile, String name) {
    Preconditions.checkArgument(testFile.type() == InputFile.Type.TEST, "Should be a test file: " + testFile);
    Preconditions.checkArgument(StringUtils.isNotBlank(name), "Test name should not be blank");
    this.testFile = testFile;
    this.name = name;
  }

  @Override
  public TestCaseBuilder durationInMs(long duration) {
    Preconditions.checkArgument(duration >= 0, "Test duration must be positive (got: " + duration + ")");
    this.duration = duration;
    return this;
  }

  @Override
  public TestCaseBuilder status(TestCase.Status status) {
    Preconditions.checkNotNull(status);
    this.status = status;
    return this;
  }

  @Override
  public TestCaseBuilder message(@Nullable String message) {
    this.message = message;
    return this;
  }

  @Override
  public TestCaseBuilder type(TestCase.Type type) {
    this.type = type;
    return this;
  }

  @Override
  public TestCaseBuilder stackTrace(@Nullable String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  @Override
  public TestCase build() {
    return new DefaultTestCase(this.testFile, this.name, this.duration, this.status, this.message, this.type, this.stackTrace);
  }

}
