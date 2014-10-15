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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.test.TestCaseCoverage;

import java.util.Collections;
import java.util.List;

public class DefaultTestCaseCoverage extends DefaultStorable implements TestCaseCoverage {

  private InputFile testFile;
  private InputFile mainFile;
  private String name;
  private List<Integer> lines;

  public DefaultTestCaseCoverage() {
    super(null);
  }

  public DefaultTestCaseCoverage(SensorStorage storage) {
    super(storage);
  }

  @Override
  public InputFile testFile() {
    return testFile;
  }

  @Override
  public DefaultTestCaseCoverage testFile(InputFile testFile) {
    Preconditions.checkNotNull(testFile, "TestFile cannot be null");
    Preconditions.checkArgument(testFile.type() == InputFile.Type.TEST, "Should be a test file: " + testFile);
    this.testFile = testFile;
    return this;
  }

  @Override
  public InputFile coveredFile() {
    return mainFile;
  }

  @Override
  public DefaultTestCaseCoverage cover(InputFile mainFile) {
    Preconditions.checkNotNull(mainFile, "InputFile cannot be null");
    Preconditions.checkArgument(mainFile.type() == InputFile.Type.MAIN, "Should be a main file: " + mainFile);
    this.mainFile = mainFile;
    return this;
  }

  @Override
  public DefaultTestCaseCoverage testName(String name) {
    Preconditions.checkArgument(StringUtils.isNotBlank(name), "Test name is mandatory and should not be blank");
    this.name = name;
    return this;
  }

  @Override
  public String testName() {
    return name;
  }

  @Override
  public List<Integer> coveredLines() {
    return Collections.unmodifiableList(lines);
  }

  @Override
  public DefaultTestCaseCoverage onLines(List<Integer> lines) {
    Preconditions.checkNotNull(lines, "Lines list cannot be null");
    Preconditions.checkArgument(!lines.isEmpty(), "No need to register test coverage if no line is covered");
    this.lines = lines;
    return this;
  }

  @Override
  public void doSave() {
    Preconditions.checkNotNull(testFile, "TestFile is mandatory");
    Preconditions.checkNotNull(mainFile, "MainFile is mandatory");
    Preconditions.checkNotNull(name, "Test name is mandatory");
    Preconditions.checkNotNull(lines, "Lines are mandatory");
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
    DefaultTestCaseCoverage rhs = (DefaultTestCaseCoverage) obj;
    return new EqualsBuilder()
      .append(testFile, rhs.testFile)
      .append(name, rhs.name)
      .append(mainFile, rhs.mainFile)
      .append(lines.toArray(), rhs.lines.toArray())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(13, 43)
      .append(testFile)
      .append(name)
      .append(mainFile)
      .toHashCode();
  }
}
