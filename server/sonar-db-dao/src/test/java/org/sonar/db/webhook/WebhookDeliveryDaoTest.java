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
package org.sonar.db.webhook;

import java.util.List;
import java.util.Map;
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

public class WebhookDeliveryDaoTest {

  private static final long NOW = 1_500_000_000L;
  private static final long BEFORE = NOW - 1_000L;

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE).setDisableDefaultOrganization(true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final WebhookDeliveryDao underTest = dbClient.webhookDeliveryDao();

  @Test
  public void selectByUuid_returns_empty_if_uuid_does_not_exist() {
    assertThat(underTest.selectByUuid(dbSession, "missing")).isEmpty();
  }

  @Test
  public void selectOrderedByComponentUuid_returns_empty_if_no_records() {
    underTest.insert(dbSession, newDto("D1", "COMPONENT_1", "TASK_1"));

    List<WebhookDeliveryLiteDto> deliveries = underTest.selectOrderedByComponentUuid(dbSession, "ANOTHER_COMPONENT");

    assertThat(deliveries).isEmpty();
  }

  @Test
  public void selectOrderedByComponentUuid_returns_records_ordered_by_date() {
    WebhookDeliveryDto dto1 = newDto("D1", "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE);
    WebhookDeliveryDto dto2 = newDto("D2", "COMPONENT_1", "TASK_1").setCreatedAt(NOW);
    WebhookDeliveryDto dto3 = newDto("D3", "COMPONENT_2", "TASK_1").setCreatedAt(NOW);
    underTest.insert(dbSession, dto3);
    underTest.insert(dbSession, dto2);
    underTest.insert(dbSession, dto1);

    List<WebhookDeliveryLiteDto> deliveries = underTest.selectOrderedByComponentUuid(dbSession, "COMPONENT_1");

    assertThat(deliveries).extracting(WebhookDeliveryLiteDto::getUuid).containsExactly("D2", "D1");
  }

  @Test
  public void selectOrderedByCeTaskUuid_returns_empty_if_no_records() {
    underTest.insert(dbSession, newDto("D1", "COMPONENT_1", "TASK_1"));

    List<WebhookDeliveryLiteDto> deliveries = underTest.selectOrderedByCeTaskUuid(dbSession, "ANOTHER_TASK");

    assertThat(deliveries).isEmpty();
  }

  @Test
  public void selectOrderedByCeTaskUuid_returns_records_ordered_by_date() {
    WebhookDeliveryDto dto1 = newDto("D1", "COMPONENT_1", "TASK_1").setCreatedAt(BEFORE);
    WebhookDeliveryDto dto2 = newDto("D2", "COMPONENT_1", "TASK_1").setCreatedAt(NOW);
    WebhookDeliveryDto dto3 = newDto("D3", "COMPONENT_2", "TASK_2").setCreatedAt(NOW);
    underTest.insert(dbSession, dto3);
    underTest.insert(dbSession, dto2);
    underTest.insert(dbSession, dto1);

    List<WebhookDeliveryLiteDto> deliveries = underTest.selectOrderedByCeTaskUuid(dbSession, "TASK_1");

    assertThat(deliveries).extracting(WebhookDeliveryLiteDto::getUuid).containsExactly("D2", "D1");
  }

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
  public void deleteComponentBeforeDate_deletes_rows_before_date() {
    underTest.insert(dbSession, newDto("DELIVERY_1", "COMPONENT_1", "TASK_1").setCreatedAt(1_000_000L));
    underTest.insert(dbSession, newDto("DELIVERY_2", "COMPONENT_1", "TASK_2").setCreatedAt(2_000_000L));
    underTest.insert(dbSession, newDto("DELIVERY_3", "COMPONENT_2", "TASK_3").setCreatedAt(1_000_000L));

    // should delete the old delivery on COMPONENT_1 and keep the one of COMPONENT_2
    underTest.deleteComponentBeforeDate(dbSession, "COMPONENT_1", 1_500_000L);

    List<Map<String, Object>> uuids = dbTester.select(dbSession, "select uuid as \"uuid\" from webhook_deliveries");
    assertThat(uuids).extracting(column -> column.get("uuid")).containsOnly("DELIVERY_2", "DELIVERY_3");
  }

  @Test
  public void deleteComponentBeforeDate_does_nothing_on_empty_table() {
    underTest.deleteComponentBeforeDate(dbSession, "COMPONENT_1", 1_500_000L);

    assertThat(dbTester.countRowsOfTable(dbSession, "webhook_deliveries")).isEqualTo(0);
  }

  @Test
  public void deleteComponentBeforeDate_does_nothing_on_invalid_uuid() {
    underTest.insert(dbSession, newDto("DELIVERY_1", "COMPONENT_1", "TASK_1").setCreatedAt(1_000_000L));

    underTest.deleteComponentBeforeDate(dbSession, "COMPONENT_2", 1_500_000L);

    assertThat(dbTester.countRowsOfTable(dbSession, "webhook_deliveries")).isEqualTo(1);
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
