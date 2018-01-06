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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateCeTaskUuidColumnToNullableOnWebhookDeliveriesTest {
  private static final String TABLE = "webhook_deliveries";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(UpdateCeTaskUuidColumnToNullableOnWebhookDeliveriesTest.class, "initial.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdateCeTaskUuidColumnToNullableOnWebhookDeliveries underTest = new UpdateCeTaskUuidColumnToNullableOnWebhookDeliveries(db.database());

  @Test
  public void update_column() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(0);
    db.assertColumnDefinition(TABLE, "ce_task_uuid", Types.VARCHAR, 40, true);
    db.assertColumnDefinition(TABLE, "analysis_uuid", Types.VARCHAR, 40, true);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();
  }
}
