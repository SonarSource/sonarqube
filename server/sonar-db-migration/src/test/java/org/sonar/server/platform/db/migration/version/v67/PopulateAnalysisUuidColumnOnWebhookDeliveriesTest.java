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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PopulateAnalysisUuidColumnOnWebhookDeliveriesTest {
  private static final String TABLE_WEBHOOK_DELIVERY = "webhook_deliveries";
  private static final String TABLE_CE_ACTIVITIY = "ce_activity";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(PopulateAnalysisUuidColumnOnWebhookDeliveriesTest.class, "initial.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PopulateAnalysisUuidColumnOnWebhookDeliveries underTest = new PopulateAnalysisUuidColumnOnWebhookDeliveries(db.database());

  @Test
  public void migration_must_set_analysis_uuid() throws SQLException {
    String ceTaskUuid = randomAlphanumeric(40);
    String analysisUuid = randomAlphanumeric(40);
    String webhookDeliveryUuid = randomAlphanumeric(40);
    insertWebhookDelivery(webhookDeliveryUuid,null, ceTaskUuid);
    insertCeActivity(ceTaskUuid, analysisUuid);

    assertThat(db.countRowsOfTable(TABLE_WEBHOOK_DELIVERY)).isEqualTo(1);
    assertThat(db.countRowsOfTable(TABLE_CE_ACTIVITIY)).isEqualTo(1);
    underTest.execute();

    List<Map<String, Object>> maps = selectAllWebhookDeliveries();
    assertThat(maps).hasSize(1);
    assertThat(maps.get(0)).containsExactly(
      entry("ANALYSIS_UUID", analysisUuid), entry("UUID", webhookDeliveryUuid), entry("CE_TASK_UUID", ceTaskUuid));
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    for (int i = 0; i < 10; i++) {
      insertWebhookDelivery(randomAlphanumeric(40),null, randomAlphanumeric(40));
      insertCeActivity(randomAlphanumeric(40), randomAlphanumeric(40));
    }

    underTest.execute();
    List<Map<String, Object>> firstExecutionResult = selectAllWebhookDeliveries();
    underTest.execute();
    assertThat(selectAllWebhookDeliveries()).isEqualTo(firstExecutionResult);
  }

  private List<Map<String, Object>> selectAllWebhookDeliveries() {
    return db.select("select uuid, ce_task_uuid, analysis_uuid from webhook_deliveries");
  }

  private void insertCeActivity(String uuid, String analysisUuid) {
    db.executeInsert(TABLE_CE_ACTIVITIY,
      "UUID", uuid,
      "TASK_TYPE", randomAlphanumeric(5),
      "ANALYSIS_UUID", analysisUuid,
      "STATUS", randomAlphanumeric(5),
      "IS_LAST", RandomUtils.nextBoolean(),
      "IS_LAST_KEY", randomAlphanumeric(50),
      "EXECUTION_COUNT", RandomUtils.nextInt(10),
      "SUBMITTED_AT", RandomUtils.nextInt(),
      "CREATED_AT", RandomUtils.nextInt(),
      "UPDATED_AT", RandomUtils.nextInt()
    );
  }

  private void insertWebhookDelivery(String uuid, @Nullable String analysisUuid, String ceTaskUuid) {
    db.executeInsert(TABLE_WEBHOOK_DELIVERY,
      "UUID", uuid,
      "COMPONENT_UUID", randomAlphanumeric(30),
      "ANALYSIS_UUID", analysisUuid,
      "CE_TASK_UUID", ceTaskUuid,
      "NAME", randomAlphanumeric(15),
      "URL", randomAlphanumeric(15),
      "SUCCESS", RandomUtils.nextBoolean(),
      "PAYLOAD", randomAlphanumeric(200),
      "CREATED_AT", RandomUtils.nextInt()
    );
  }
}
