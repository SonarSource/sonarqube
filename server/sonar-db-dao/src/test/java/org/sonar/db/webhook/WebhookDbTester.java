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
package org.sonar.db.webhook;

import java.util.Optional;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.webhook.WebhookTesting.newWebhook;

public class WebhookDbTester {

  private final DbTester dbTester;

  public WebhookDbTester(DbTester dbTester) {
    this.dbTester = dbTester;
  }

  public WebhookDto insertWebhook(OrganizationDto organizationDto) {
    return insert(newWebhook(organizationDto));
  }

  public WebhookDto insertWebhook(ComponentDto project) {
    return insert(newWebhook(project));
  }

  public WebhookDto insert(WebhookDto dto) {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().webhookDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  public Optional<WebhookDto> selectWebhook(String uuid) {
    DbSession dbSession = dbTester.getSession();
    return dbTester.getDbClient().webhookDao().selectByUuid(dbSession, uuid);
  }
}
