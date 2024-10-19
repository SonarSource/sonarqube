/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class TestPlanBuilder {
  private final Map<InputFile, DefaultTestPlan> testPlanByFile = new HashMap<>();

  public DefaultTestPlan getTestPlan(InputFile component) {
    DefaultInputFile inputFile = (DefaultInputFile) component;
    inputFile.setPublished(true);
    if (!testPlanByFile.containsKey(inputFile)) {
      testPlanByFile.put(inputFile, new DefaultTestPlan());
    }
    return testPlanByFile.get(inputFile);
  }

  @CheckForNull
  public DefaultTestPlan getTestPlanByFile(InputFile inputFile) {
    return testPlanByFile.get(inputFile);
  }
}
