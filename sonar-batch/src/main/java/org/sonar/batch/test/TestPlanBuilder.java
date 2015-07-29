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
package org.sonar.batch.test;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.batch.deprecated.perspectives.PerspectiveBuilder;
import org.sonar.batch.index.BatchComponent;

public class TestPlanBuilder extends PerspectiveBuilder<MutableTestPlan> {

  private Map<InputFile, DefaultTestPlan> testPlanByFile = new HashMap<>();

  public TestPlanBuilder() {
    super(MutableTestPlan.class);
  }

  @CheckForNull
  @Override
  public MutableTestPlan loadPerspective(Class<MutableTestPlan> perspectiveClass, BatchComponent component) {
    if (component.isFile()) {
      InputFile inputFile = (InputFile) component.inputComponent();
      if (inputFile.type() == Type.TEST) {
        if (!testPlanByFile.containsKey(inputFile)) {
          testPlanByFile.put(inputFile, new DefaultTestPlan());
        }
        return testPlanByFile.get(inputFile);
      }
    }
    return null;
  }

}
