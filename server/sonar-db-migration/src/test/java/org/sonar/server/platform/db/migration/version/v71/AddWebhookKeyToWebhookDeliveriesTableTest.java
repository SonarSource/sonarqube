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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.CoreDbTester.createForSchema;

public class AddWebhookKeyToWebhookDeliveriesTableTest {
  @Rule
  public final CoreDbTester dbTester = createForSchema(AddWebhookKeyToWebhookDeliveriesTableTest.class, "webhook-deliveries.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddWebhookKeyToWebhookDeliveriesTable underTest = new AddWebhookKeyToWebhookDeliveriesTable(dbTester.database());

  @Test
  public void table_has_been_truncated() throws SQLException {

    insertDelivery();

    underTest.execute();

    int count = dbTester.countRowsOfTable("webhook_deliveries");
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void column_webhook_uuid_has_been_added_to_table() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("webhook_deliveries", "webhook_uuid", VARCHAR, 40, false);
  }

  @Test
  public void column_duration_ms_is_now_not_nullable() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("webhook_deliveries", "duration_ms", BIGINT, null, false);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

  private void insertDelivery() {
    dbTester.executeInsert("webhook_deliveries",
      "uuid", randomAlphanumeric(40),
//      "webhook_uuid", randomAlphanumeric(40),
      "component_uuid", randomAlphanumeric(40),
      "name", randomAlphabetic(60),
      "url", randomAlphabetic(200),
      "success", true,
      "duration_ms", randomNumeric(7),
      "payload", randomAlphanumeric(1000),
      "created_at", valueOf(1_55555_555));
  }

}
