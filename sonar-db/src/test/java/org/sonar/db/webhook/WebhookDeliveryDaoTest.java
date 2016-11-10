/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.webhook;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.webhook.WebhookDbTesting.newWebhookDeliveryDto;
import static org.sonar.db.webhook.WebhookDbTesting.selectAllDeliveryUuids;


public class WebhookDeliveryDaoTest {

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE).setDisableDefaultOrganization(true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final WebhookDeliveryDao underTest = dbClient.webhookDeliveryDao();

  @Test
  public void insert_row_with_only_mandatory_columns() {
    WebhookDeliveryDto dto = newDto("DELIVERY_1", "COMPONENT_1", "TASK_1")
      .setHttpStatus(null)
      .setDurationMs(null)
      .setErrorStacktrace(null);

    underTest.insert(dbSession, dto);

    WebhookDeliveryDto stored = selectByUuid(dto.getUuid());
    verifyMandatoryFields(dto, stored);

    // optional fields are null
    assertThat(stored.getHttpStatus()).isNull();
    assertThat(stored.getDurationMs()).isNull();
    assertThat(stored.getErrorStacktrace()).isNull();
  }

  @Test
  public void insert_row_with_all_columns() {
    WebhookDeliveryDto dto = newDto("DELIVERY_1", "COMPONENT_1", "TASK_1");

    underTest.insert(dbSession, dto);

    WebhookDeliveryDto stored = selectByUuid(dto.getUuid());
    verifyMandatoryFields(dto, stored);
    assertThat(stored.getHttpStatus()).isEqualTo(dto.getHttpStatus());
    assertThat(stored.getDurationMs()).isEqualTo(dto.getDurationMs());
    assertThat(stored.getErrorStacktrace()).isEqualTo(dto.getErrorStacktrace());
  }

  @Test
  public void delete_rows_before_date() {
    underTest.insert(dbSession, newDto("DELIVERY_1", "COMPONENT_1", "TASK_1").setCreatedAt(1_000_000L));
    underTest.insert(dbSession, newDto("DELIVERY_2", "COMPONENT_1", "TASK_2").setCreatedAt(2_000_000L));
    underTest.insert(dbSession, newDto("DELIVERY_3", "COMPONENT_2", "TASK_3").setCreatedAt(1_000_000L));

    // should delete the old delivery on COMPONENT_1 and keep the one of COMPONENT_2
    underTest.deleteComponentBeforeDate(dbSession, "COMPONENT_1", 1_500_000L);

    assertThat(selectAllDeliveryUuids(dbTester, dbSession)).containsOnly("DELIVERY_2", "DELIVERY_3");

  }

  private void verifyMandatoryFields(WebhookDeliveryDto expected, WebhookDeliveryDto actual) {
    assertThat(actual.getUuid()).isEqualTo(expected.getUuid());
    assertThat(actual.getComponentUuid()).isEqualTo(expected.getComponentUuid());
    assertThat(actual.getCeTaskUuid()).isEqualTo(expected.getCeTaskUuid());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
    assertThat(actual.isSuccess()).isEqualTo(expected.isSuccess());
    assertThat(actual.getPayload()).isEqualTo(expected.getPayload());
    assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
  }

  /**
   * Build a {@link WebhookDeliveryDto} with all mandatory fields.
   * Optional fields are kept null.
   */
  private static WebhookDeliveryDto newDto(String uuid, String componentUuid, String ceTaskUuid) {
    return newWebhookDeliveryDto()
      .setUuid(uuid)
      .setComponentUuid(componentUuid)
      .setCeTaskUuid(ceTaskUuid);
  }

  private WebhookDeliveryDto selectByUuid(String uuid) {
    Optional<WebhookDeliveryDto> dto = underTest.selectByUuid(dbSession, uuid);
    assertThat(dto).isPresent();
    return dto.get();
  }
}
