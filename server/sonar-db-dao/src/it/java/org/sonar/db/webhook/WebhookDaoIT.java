/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDaoIT {

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final System2 system2 = System2.INSTANCE;
  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final WebhookDao underTest = dbClient.webhookDao();
  private final WebhookDbTester webhookDbTester = dbTester.webhooks();
  private final ComponentDbTester componentDbTester = dbTester.components();

  @Test
  void selectByUuid_returns_empty_if_uuid_does_not_exist() {
    assertThat(underTest.selectByUuid(dbSession, "missing")).isEmpty();
  }

  @Test
  void select_global_webhooks() {
    ProjectDto projectDto = componentDbTester.insertPrivateProject().getProjectDto();
    webhookDbTester.insertGlobalWebhook();
    webhookDbTester.insertGlobalWebhook();
    webhookDbTester.insertWebhook(projectDto);
    webhookDbTester.insertWebhook(projectDto);

    List<WebhookDto> results = underTest.selectGlobalWebhooks(dbSession);

    assertThat(results).hasSize(2);
  }

  @Test
  void select_global_webhooks_returns_empty_list_if_there_are_no_global_webhooks() {
    ProjectDto projectDto = componentDbTester.insertPrivateProject().getProjectDto();
    webhookDbTester.insertWebhook(projectDto);
    webhookDbTester.insertWebhook(projectDto);

    List<WebhookDto> results = underTest.selectGlobalWebhooks(dbSession);

    assertThat(results).isEmpty();
  }

  @Test
  void insert_global_webhook() {
    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1")
      .setSecret("a_secret");

    underTest.insert(dbSession, dto, null, null);

    WebhookDto stored = selectByUuid(dto.getUuid());

    assertThat(stored.getUuid()).isEqualTo(dto.getUuid());
    assertThat(stored.getName()).isEqualTo(dto.getName());
    assertThat(stored.getUrl()).isEqualTo(dto.getUrl());
    assertThat(stored.getProjectUuid()).isNull();
    assertThat(stored.getSecret()).isEqualTo(dto.getSecret());
    assertThat(new Date(stored.getCreatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
    assertThat(new Date(stored.getUpdatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
  }

  @Test
  void insert_row_with_project() {
    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1")
      .setProjectUuid("UUID_2")
      .setSecret("a_secret");

    underTest.insert(dbSession, dto, "project_key", "project_name");

    WebhookDto reloaded = selectByUuid(dto.getUuid());

    assertThat(reloaded.getUuid()).isEqualTo(dto.getUuid());
    assertThat(reloaded.getName()).isEqualTo(dto.getName());
    assertThat(reloaded.getUrl()).isEqualTo(dto.getUrl());
    assertThat(reloaded.getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(reloaded.getSecret()).isEqualTo(dto.getSecret());
    assertThat(new Date(reloaded.getCreatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
    assertThat(new Date(reloaded.getUpdatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
  }

  @Test
  void update_with_only_required_fields() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();

    underTest.update(dbSession, dto
        .setName("a-fancy-webhook")
        .setUrl("http://www.fancy-webhook.io")
        .setSecret(null),
      null, null);

    Optional<WebhookDto> optionalResult = underTest.selectByUuid(dbSession, dto.getUuid());
    assertThat(optionalResult).isPresent();
    WebhookDto reloaded = optionalResult.get();
    assertThat(reloaded.getUuid()).isEqualTo(dto.getUuid());
    assertThat(reloaded.getName()).isEqualTo("a-fancy-webhook");
    assertThat(reloaded.getUrl()).isEqualTo("http://www.fancy-webhook.io");
    assertThat(reloaded.getProjectUuid()).isNull();
    assertThat(reloaded.getSecret()).isNull();
    assertThat(reloaded.getCreatedAt()).isEqualTo(dto.getCreatedAt());
    assertThat(new Date(reloaded.getUpdatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
  }

  @Test
  void update_with_all_fields() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();

    underTest.update(dbSession, dto
        .setName("a-fancy-webhook")
        .setUrl("http://www.fancy-webhook.io")
        .setSecret("a_new_secret"),
      null, null);

    Optional<WebhookDto> optionalResult = underTest.selectByUuid(dbSession, dto.getUuid());
    assertThat(optionalResult).isPresent();
    WebhookDto reloaded = optionalResult.get();
    assertThat(reloaded.getUuid()).isEqualTo(dto.getUuid());
    assertThat(reloaded.getName()).isEqualTo("a-fancy-webhook");
    assertThat(reloaded.getUrl()).isEqualTo("http://www.fancy-webhook.io");
    assertThat(reloaded.getProjectUuid()).isNull();
    assertThat(reloaded.getSecret()).isEqualTo("a_new_secret");
    assertThat(reloaded.getCreatedAt()).isEqualTo(dto.getCreatedAt());
    assertThat(new Date(reloaded.getUpdatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
  }

  @Test
  void cleanWebhooksOfAProject() {
    ProjectDto projectDto = componentDbTester.insertPrivateProject().getProjectDto();
    webhookDbTester.insertWebhook(projectDto);
    webhookDbTester.insertWebhook(projectDto);
    webhookDbTester.insertWebhook(projectDto);
    webhookDbTester.insertWebhook(projectDto);

    underTest.deleteByProject(dbSession, projectDto);

    Optional<WebhookDto> reloaded = underTest.selectByUuid(dbSession, projectDto.getUuid());
    assertThat(reloaded).isEmpty();
  }

  @Test
  void delete() {
    WebhookDto dto = webhookDbTester.insertGlobalWebhook();

    underTest.delete(dbSession, dto.getUuid(), dto.getName());

    Optional<WebhookDto> reloaded = underTest.selectByUuid(dbSession, dto.getUuid());
    assertThat(reloaded).isEmpty();
  }

  @Test
  void set_default_org_if_webhook_does_not_have_a_project() {
    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1");

    underTest.insert(dbSession, dto, null, null);

    Optional<WebhookDto> reloaded = underTest.selectByUuid(dbSession, dto.getUuid());
    assertThat(reloaded).isPresent();
  }

  private WebhookDto selectByUuid(String uuid) {
    Optional<WebhookDto> dto = underTest.selectByUuid(dbSession, uuid);
    assertThat(dto).isPresent();
    return dto.get();
  }

}
