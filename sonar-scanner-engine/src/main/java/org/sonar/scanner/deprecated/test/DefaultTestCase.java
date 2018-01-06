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
package org.sonar.scanner.deprecated.test;

import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.api.test.exception.CoverageAlreadyExistsException;
import org.sonar.api.test.exception.IllegalDurationException;

public class DefaultTestCase implements MutableTestCase {

  private final DefaultTestPlan testPlan;
  private String type;
  private Long durationInMs;
  private Status status;
  private String name;
  private String message;
  private String stackTrace;
  private Map<DefaultInputFile, CoverageBlock> coverageBlocksByTestedFile = new LinkedHashMap<>();

  public DefaultTestCase(DefaultTestPlan testPlan) {
    this.testPlan = testPlan;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public MutableTestCase setType(@Nullable String s) {
    this.type = s;
    return this;
  }

  @Override
  public Long durationInMs() {
    return durationInMs;
  }

  @Override
  public MutableTestCase setDurationInMs(@Nullable Long l) {
    if (l != null && l < 0) {
      throw new IllegalDurationException("Test duration must be positive (got: " + l + ")");
    }
    this.durationInMs = l;
    return this;
  }

  @Override
  public Status status() {
    return status;
  }

  @Override
  public MutableTestCase setStatus(@Nullable Status s) {
    this.status = s;
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  public MutableTestCase setName(String s) {
    this.name = s;
    return this;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public MutableTestCase setMessage(String s) {
    this.message = s;
    return this;
  }

  @Override
  public String stackTrace() {
    return stackTrace;
  }

  @Override
  public MutableTestCase setStackTrace(String s) {
    this.stackTrace = s;
    return this;
  }

  @Override
  public MutableTestCase setCoverageBlock(Testable testable, List<Integer> lines) {
    DefaultInputFile coveredFile = ((DefaultTestable) testable).inputFile();
    return setCoverageBlock(coveredFile, lines);
  }

  @Override
  public MutableTestCase setCoverageBlock(InputFile mainFile, List<Integer> lines) {
    Preconditions.checkArgument(mainFile.type() == Type.MAIN, "Test file can only cover a main file");
    DefaultInputFile coveredFile = (DefaultInputFile) mainFile;
    if (coverageBlocksByTestedFile.containsKey(coveredFile)) {
      throw new CoverageAlreadyExistsException("The link between " + name() + " and " + coveredFile.key() + " already exists");
    }
    coverageBlocksByTestedFile.put(coveredFile, new DefaultCoverageBlock(this, coveredFile, lines));
    return this;
  }

  @Override
  public TestPlan testPlan() {
    return testPlan;
  }

  @Override
  public boolean doesCover() {
    return !coverageBlocksByTestedFile.isEmpty();
  }

  @Override
  public int countCoveredLines() {
    throw new UnsupportedOperationException("Not supported since SQ 5.2");
  }

  @Override
  public Iterable<CoverageBlock> coverageBlocks() {
    return coverageBlocksByTestedFile.values();
  }

  @Override
  public CoverageBlock coverageBlock(final Testable testable) {
    DefaultInputFile coveredFile = ((DefaultTestable) testable).inputFile();
    return coverageBlocksByTestedFile.get(coveredFile);
  }

}
