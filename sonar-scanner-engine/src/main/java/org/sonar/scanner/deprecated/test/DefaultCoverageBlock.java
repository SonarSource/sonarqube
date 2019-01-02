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
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.Testable;

public class DefaultCoverageBlock implements CoverageBlock {

  private final TestCase testCase;
  private final DefaultInputFile testable;
  private final List<Integer> lines;

  public DefaultCoverageBlock(TestCase testCase, DefaultInputFile testable, List<Integer> lines) {
    this.testCase = testCase;
    this.testable = testable;
    this.lines = lines;
  }

  @Override
  public TestCase testCase() {
    return testCase;
  }

  @Override
  public Testable testable() {
    return new DefaultTestable(testable);
  }

  @Override
  public List<Integer> lines() {
    return lines;
  }
}
