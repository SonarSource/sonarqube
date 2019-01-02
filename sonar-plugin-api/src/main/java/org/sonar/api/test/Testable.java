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
package org.sonar.api.test;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.sonar.api.component.Perspective;

/**
 * @deprecated since 5.2
 */
@Deprecated
public interface Testable extends Perspective {

  List<TestCase> testCases();

  TestCase testCaseByName(String key);

  int countTestCasesOfLine(Integer line);

  Map<Integer, Integer> testCasesByLines();

  List<TestCase> testCasesOfLine(int line);

  SortedSet<Integer> testedLines();

  CoverageBlock coverageBlock(TestCase testCase);

  Iterable<CoverageBlock> coverageBlocks();
}
