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
package org.sonar.server.platform.db.migration;

import org.junit.Test;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DatabaseMigrationImplAsynchronousTest {

  private boolean taskSuppliedForAsyncProcess = false;
  /**
   * Implementation of execute wraps specified Runnable to add a delay of 200 ms before passing it
   * to a SingleThread executor to execute asynchronously.
   */
  private DatabaseMigrationExecutorService executorService = new DatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(final Runnable command) {
      taskSuppliedForAsyncProcess = true;
    }
  };
  private MutableDatabaseMigrationState migrationState = mock(MutableDatabaseMigrationState.class);
  private Platform platform = mock(Platform.class);
  private MigrationEngine migrationEngine = mock(MigrationEngine.class);
  private DatabaseMigrationImpl underTest = new DatabaseMigrationImpl(executorService, migrationState, migrationEngine, platform);

  @Test
  public void testName() {
    underTest.startIt();

    assertThat(taskSuppliedForAsyncProcess).isTrue();
  }
}
