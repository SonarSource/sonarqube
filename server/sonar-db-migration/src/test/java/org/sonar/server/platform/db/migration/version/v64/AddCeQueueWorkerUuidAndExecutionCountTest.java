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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class AddCeQueueWorkerUuidAndExecutionCountTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddCeQueueWorkerUuidAndExecutionCountTest.class, "ce_queue.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddCeQueueWorkerUuidAndExecutionCount underTest = new AddCeQueueWorkerUuidAndExecutionCount(db.database());

  @Test
  public void execute_adds_columns_worker_uuid_and_processing_count() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("ce_queue", "worker_uuid", Types.VARCHAR, 40, true);
    db.assertColumnDefinition("ce_queue", "execution_count", Types.INTEGER, null, true);
  }

  @Test
  public void execute_is_not_reentreant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }
}
