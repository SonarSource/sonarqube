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
package org.sonar.scanner.deprecated.test;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;

public class DefaultTestable implements MutableTestable {

  private final DefaultInputFile inputFile;

  public DefaultTestable(DefaultInputFile inputFile) {
    this.inputFile = inputFile;
  }

  public DefaultInputFile inputFile() {
    return inputFile;
  }

  @Override
  public List<TestCase> testCases() {
    throw unsupported();
  }

  @Override
  public TestCase testCaseByName(final String name) {
    throw unsupported();
  }

  @Override
  public int countTestCasesOfLine(Integer line) {
    throw unsupported();
  }

  @Override
  public Map<Integer, Integer> testCasesByLines() {
    throw unsupported();
  }

  @Override
  public List<TestCase> testCasesOfLine(int line) {
    throw unsupported();
  }

  @Override
  public SortedSet<Integer> testedLines() {
    throw unsupported();
  }

  @Override
  public CoverageBlock coverageBlock(final TestCase testCase) {
    throw unsupported();
  }

  @Override
  public Iterable<CoverageBlock> coverageBlocks() {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("No more available since SQ 5.2");
  }

}
