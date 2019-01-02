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
package org.sonar.server.platform.db.migration.version;

import java.util.stream.Stream;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;

public interface DbVersion {
  /**
   * Components (if any) supporting the {@link org.sonar.server.platform.db.migration.step.MigrationStep} classes
   * added to the registry in {@link #addSteps(MigrationStepRegistry)}.
   * <p>
   * These components will be added to the {@link org.sonar.server.platform.db.migration.engine.MigrationContainer} in
   * which the {@link org.sonar.server.platform.db.migration.step.MigrationStep} classes will be instantiated and run.
   * </p>
   */
  default Stream<Object> getSupportComponents() {
    return Stream.empty();
  }

  void addSteps(MigrationStepRegistry registry);
}
