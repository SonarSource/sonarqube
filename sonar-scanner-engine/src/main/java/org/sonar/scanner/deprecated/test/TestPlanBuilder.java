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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.scanner.deprecated.perspectives.PerspectiveBuilder;

public class TestPlanBuilder extends PerspectiveBuilder<MutableTestPlan> {

  private Map<InputFile, DefaultTestPlan> testPlanByFile = new HashMap<>();

  public TestPlanBuilder() {
    super(MutableTestPlan.class);
  }

  @CheckForNull
  @Override
  public MutableTestPlan loadPerspective(Class<MutableTestPlan> perspectiveClass, InputComponent component) {
    if (component.isFile()) {
      DefaultInputFile inputFile = (DefaultInputFile) component;
      if (inputFile.type() == Type.TEST) {
        inputFile.setPublished(true);
        if (!testPlanByFile.containsKey(inputFile)) {
          testPlanByFile.put(inputFile, new DefaultTestPlan());
        }
        return testPlanByFile.get(inputFile);
      }
    }
    return null;
  }

  @CheckForNull
  public DefaultTestPlan getTestPlanByFile(InputFile inputFile) {
    return testPlanByFile.get(inputFile);
  }

}
