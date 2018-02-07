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

import java.util.Date;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookDaoTest {

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE).setDisableDefaultOrganization(true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = System2.INSTANCE;

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final WebhookDao underTest = dbClient.webhookDao();

  @Test
  public void selectByUuid_returns_empty_if_uuid_does_not_exist() {
    assertThat(underTest.selectByUuid(dbSession, "missing")).isEmpty();
  }

  @Test
  public void insert_row_with_organization() {

    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1")
      .setOrganizationUuid("UUID_2");

    underTest.insert(dbSession, dto);

    WebhookDto stored = selectByUuid(dto.getUuid());

    assertThat(stored.getUuid()).isEqualTo(dto.getUuid());
    assertThat(stored.getName()).isEqualTo(dto.getName());
    assertThat(stored.getUrl()).isEqualTo(dto.getUrl());
    assertThat(stored.getOrganizationUuid()).isEqualTo(dto.getOrganizationUuid());
    assertThat(stored.getProjectUuid()).isNull();
    assertThat(new Date(stored.getCreatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
    assertThat(stored.getUpdatedAt()).isNull();
  }

  @Test
  public void insert_row_with_project() {

    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1")
      .setProjectUuid("UUID_2");

    underTest.insert(dbSession, dto);

    WebhookDto stored = selectByUuid(dto.getUuid());

    assertThat(stored.getUuid()).isEqualTo(dto.getUuid());
    assertThat(stored.getName()).isEqualTo(dto.getName());
    assertThat(stored.getUrl()).isEqualTo(dto.getUrl());
    assertThat(stored.getOrganizationUuid()).isNull();
    assertThat(stored.getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(new Date(stored.getCreatedAt())).isInSameMinuteWindowAs(new Date(system2.now()));
    assertThat(stored.getUpdatedAt()).isNull();
  }

  @Test
  public void fail_if_webhook_does_not_have_an_organization_nor_a_project() {

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("A webhook can not be created if not linked to an organization or a project.");

    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1");

    underTest.insert(dbSession, dto);

  }

  @Test
  public void fail_if_webhook_have_both_an_organization_nor_a_project() {

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("A webhook can not be linked to both an organization and a project.");

    WebhookDto dto = new WebhookDto()
      .setUuid("UUID_1")
      .setName("NAME_1")
      .setUrl("URL_1")
      .setOrganizationUuid("UUID_2")
      .setProjectUuid("UUID_3");

    underTest.insert(dbSession, dto);

  }

  private WebhookDto selectByUuid(String uuid) {
    Optional<WebhookDto> dto = underTest.selectByUuid(dbSession, uuid);
    assertThat(dto).isPresent();
    return dto.get();
  }

}
