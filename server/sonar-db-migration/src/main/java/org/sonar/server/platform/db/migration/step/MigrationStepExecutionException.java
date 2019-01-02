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
package org.sonar.server.platform.db.migration.step;

import static java.util.Objects.requireNonNull;

public class MigrationStepExecutionException extends RuntimeException {
  private final transient RegisteredMigrationStep failingStep;

  public MigrationStepExecutionException(RegisteredMigrationStep failingStep, Throwable cause) {
    super(createMessage(failingStep), requireNonNull(cause, "cause can't be null"));
    this.failingStep = failingStep;
  }

  private static String createMessage(RegisteredMigrationStep failingStep) {
    check(failingStep);
    return String.format("Execution of migration step %s failed", failingStep);
  }

  private static RegisteredMigrationStep check(RegisteredMigrationStep failingStep) {
    return requireNonNull(failingStep, "RegisteredMigrationStep can't be null");
  }

  public RegisteredMigrationStep getFailingStep() {
    return failingStep;
  }
}
