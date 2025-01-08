/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.engine;

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.platform.LazyStrategy;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;

import static java.util.Collections.emptyList;

public class MigrationContainerImpl extends SpringComponentContainer implements MigrationContainer {

  public MigrationContainerImpl(SpringComponentContainer parent, Class<? extends MigrationStepsExecutor> executor) {
    super(parent, parent.getComponentByType(PropertyDefinitions.class), emptyList(), new LazyStrategy());
    add(executor);
    addSteps(parent.getComponentByType(MigrationSteps.class));
    startComponents();
  }

  private void addSteps(MigrationSteps migrationSteps) {
    Set<Class<? extends MigrationStep>> classes = new HashSet<>();
    migrationSteps.readAll().forEach(step -> {
      Class<? extends MigrationStep> stepClass = step.getStepClass();
      if (!classes.contains(stepClass)) {
        add(stepClass);
        classes.add(stepClass);
      }
    });
  }

  @Override
  public void cleanup() {
    stopComponents();
  }

  @Override
  public String toString() {
    return "MigrationContainerImpl";
  }
}
