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
package org.sonar.server.platform.db.migration.engine;

import java.util.HashSet;
import java.util.Set;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutorImpl;

/**
 * Responsible for:
 * <ul>
 *   <li>adding all the {@link MigrationStep} classes to the container after building it</li>
 *   <li>adding the {@link MigrationStepsExecutorImpl} to the container</li>
 * </ul>
 */
public class MigrationContainerPopulatorImpl implements MigrationContainerPopulator {
  private final Class<? extends MigrationStepsExecutor> executorType;

  public MigrationContainerPopulatorImpl() {
    this(MigrationStepsExecutorImpl.class);
  }

  protected MigrationContainerPopulatorImpl(Class<? extends MigrationStepsExecutor> executorType) {
    this.executorType = executorType;
  }

  @Override
  public void populateContainer(MigrationContainer container) {
    container.add(executorType);
    populateFromMigrationSteps(container);
  }

  private static void populateFromMigrationSteps(MigrationContainer container) {
    MigrationSteps migrationSteps = container.getComponentByType(MigrationSteps.class);
    Set<Class<? extends MigrationStep>> classes = new HashSet<>();
    migrationSteps.readAll().forEach(step -> {
      Class<? extends MigrationStep> stepClass = step.getStepClass();
      if (!classes.contains(stepClass)) {
        container.add(stepClass);
        classes.add(stepClass);
      }
    });
  }
}
