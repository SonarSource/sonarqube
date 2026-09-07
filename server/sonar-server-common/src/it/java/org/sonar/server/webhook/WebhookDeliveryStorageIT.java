/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.webhook;

import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.webhook.WebhookDeliveryDto;
import org.sonar.db.webhook.WebhookDeliveryTesting;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.webhook.WebhookDeliveryTesting.selectAllDeliveryUuids;

public class WebhookDeliveryStorageIT {

  private static final String DELIVERY_UUID = "abcde1234";
  private static final long NOW = 1_500_000_000_000L;
  private static final long TWO_MONTHS_AGO = NOW - 60L * 24 * 60 * 60 * 1000;
  private static final long TWO_WEEKS_AGO = NOW - 14L * 24 * 60 * 60 * 1000;

  private final System2 system = mock(System2.class);

  @Rule
  public final DbTester dbTester = DbTester.create(system);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private WebhookDeliveryStorage underTest = new WebhookDeliveryStorage(dbClient, system, uuidFactory);

  @Test
  public void persist_generates_uuid_then_inserts_record() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    WebhookDelivery delivery = newBuilderTemplate().build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getUuid()).isEqualTo(DELIVERY_UUID);
    assertThat(dto.getWebhookUuid()).isEqualTo("WEBHOOK_UUID_1");
    assertThat(dto.getProjectUuid()).isEqualTo(delivery.getWebhook().getProjectUuid());
    assertThat(dto.getCeTaskUuid()).isEqualTo(delivery.getWebhook().getCeTaskUuid().get());
    assertThat(dto.getName()).isEqualTo(delivery.getWebhook().getName());
    assertThat(dto.getUrl()).isEqualTo(delivery.getWebhook().getUrl());
    assertThat(dto.getCreatedAt()).isEqualTo(delivery.getAt());
    assertThat(dto.getHttpStatus()).isEqualTo(delivery.getHttpStatus().get());
    assertThat(dto.getDurationMs()).isEqualTo(delivery.getDurationInMs().get());
    assertThat(dto.getPayload()).isEqualTo(delivery.getPayload().getJson());
    assertThat(dto.getErrorStacktrace()).isNull();
  }

  @Test
  public void persist_error_stacktrace_keeps_only_exception_class_name() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(new IOException("Unexpected status line: 220 mail.internal ESMTP Service ready"))
      .build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getErrorStacktrace())
      .isEqualTo("java.io.IOException")
      .doesNotContain("mail.internal", "ESMTP", "Service ready");
  }

  @Test
  public void persist_error_stacktrace_unwraps_nested_cause_and_keeps_only_root_class_name() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    IOException rootCause = new IOException("Unexpected status line: 220 mail.internal ESMTP Service ready");
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(new RuntimeException("wrapped failure", rootCause))
      .build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getErrorStacktrace())
      .isEqualTo("java.io.IOException")
      .doesNotContain("wrapped failure", "mail.internal", "ESMTP", "Service ready");
  }

  @Test
  public void persist_error_stacktrace_keeps_message_for_internal_validation_exceptions() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(new IllegalArgumentException(WebhookAddressValidator.INVALID_ADDRESS_MESSAGE))
      .build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getErrorStacktrace())
      .isEqualTo("java.lang.IllegalArgumentException: " + WebhookAddressValidator.INVALID_ADDRESS_MESSAGE);
  }

  @Test
  public void persist_error_stacktrace_keeps_message_for_unknown_host_exception() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    UnknownHostException error = new UnknownHostException("does_not_exist: Name or service not known");
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(error)
      .build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getErrorStacktrace())
      .isEqualTo("java.net.UnknownHostException: " + error.getMessage());
  }

  @Test
  public void purge_deletes_records_older_than_one_month() {
    when(system.now()).thenReturn(NOW);
    dbClient.webhookDeliveryDao().insert(dbSession, newDto("D1", "PROJECT_1", TWO_MONTHS_AGO));
    dbClient.webhookDeliveryDao().insert(dbSession, newDto("D2", "PROJECT_1", TWO_WEEKS_AGO));
    dbClient.webhookDeliveryDao().insert(dbSession, newDto("D3", "PROJECT_2", TWO_MONTHS_AGO));
    dbClient.webhookDeliveryDao().insert(dbSession, newDto("D4", "PROJECT_2", TWO_WEEKS_AGO));
    dbSession.commit();

    underTest.purge();

    assertThat(selectAllDeliveryUuids(dbTester, dbSession)).containsOnly("D2", "D4");
  }

  @Test
  public void persist_effective_url_if_present() {
    when(uuidFactory.create()).thenReturn(DELIVERY_UUID);
    String effectiveUrl = secure().nextAlphabetic(15);
    WebhookDelivery delivery = newBuilderTemplate()
      .setEffectiveUrl(effectiveUrl)
      .build();

    underTest.persist(delivery);

    WebhookDeliveryDto dto = dbClient.webhookDeliveryDao().selectByUuid(dbSession, DELIVERY_UUID).get();
    assertThat(dto.getUrl()).isEqualTo(effectiveUrl);
  }

  private static WebhookDelivery.Builder newBuilderTemplate() {
    return new WebhookDelivery.Builder()
      .setWebhook(new Webhook("WEBHOOK_UUID_1", "COMPONENT1", "TASK1", RandomStringUtils.secure().nextAlphanumeric(40),"Jenkins", "http://jenkins", null))
      .setPayload(new WebhookPayload("my-project", "{json}"))
      .setAt(1_000_000L)
      .setHttpStatus(200)
      .setDurationInMs(1_000);
  }

  private static WebhookDeliveryDto newDto(String uuid, String componentUuid, long at) {
    return WebhookDeliveryTesting.newDto()
      .setUuid(uuid)
      .setProjectUuid(componentUuid)
      .setCreatedAt(at);
  }
}
