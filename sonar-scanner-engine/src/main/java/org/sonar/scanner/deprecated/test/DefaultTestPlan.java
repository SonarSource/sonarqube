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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;

public class DefaultTestPlan implements MutableTestPlan {
  private List<MutableTestCase> testCases = new ArrayList<>();

  @Override
  @CheckForNull
  public Iterable<MutableTestCase> testCasesByName(String name) {
    List<MutableTestCase> result = new ArrayList<>();
    for (MutableTestCase testCase : testCases()) {
      if (name.equals(testCase.name())) {
        result.add(testCase);
      }
    }
    return result;
  }

  @Override
  public MutableTestCase addTestCase(String name) {
    DefaultTestCase testCase = new DefaultTestCase(this);
    testCase.setName(name);
    testCases.add(testCase);
    return testCase;
  }

  @Override
  public Iterable<MutableTestCase> testCases() {
    return testCases;
  }

}
