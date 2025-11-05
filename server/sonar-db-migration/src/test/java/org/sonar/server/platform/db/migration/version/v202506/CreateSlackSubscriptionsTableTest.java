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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_CHANNEL_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_RESOURCE_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_RESOURCE_TYPE;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_SLACK_WORKSPACE_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_USER_BINDING_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackSubscriptionsTable.SLACK_SUBSCRIPTIONS_TABLE_NAME;

class CreateSlackSubscriptionsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateSlackSubscriptionsTable.class);

  private final CreateSlackSubscriptionsTable underTest = new CreateSlackSubscriptionsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(SLACK_SUBSCRIPTIONS_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(SLACK_SUBSCRIPTIONS_TABLE_NAME);
    db.assertPrimaryKey(SLACK_SUBSCRIPTIONS_TABLE_NAME, "pk_slack_subscriptions", COLUMN_UUID);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_USER_BINDING_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_SLACK_WORKSPACE_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_CHANNEL_ID, Types.VARCHAR, 255, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_RESOURCE_TYPE, Types.VARCHAR, 20, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_RESOURCE_ID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(SLACK_SUBSCRIPTIONS_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);

    db.assertIndex(SLACK_SUBSCRIPTIONS_TABLE_NAME, "ss_user_binding_uuid", COLUMN_USER_BINDING_UUID);
    db.assertIndex(SLACK_SUBSCRIPTIONS_TABLE_NAME, "ss_slack_workspace_uuid", COLUMN_SLACK_WORKSPACE_UUID);
    db.assertIndex(SLACK_SUBSCRIPTIONS_TABLE_NAME, "ss_resource_type_id", COLUMN_RESOURCE_TYPE, COLUMN_RESOURCE_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(SLACK_SUBSCRIPTIONS_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(SLACK_SUBSCRIPTIONS_TABLE_NAME);
  }

}
