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
package org.sonar.db.webhook;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.SecretNewValue;
import org.sonar.db.audit.model.WebhookNewValue;
import org.sonar.db.project.ProjectDto;

public class WebhookDao implements Dao {

  private final System2 system2;
  private final AuditPersister auditPersister;

  public WebhookDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public List<WebhookDto> selectGlobalWebhooks(DbSession dbSession) {
    return mapper(dbSession).selectGlobalWebhooksOrderedByName();
  }

  public Optional<WebhookDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<WebhookDto> selectByProject(DbSession dbSession, ProjectDto projectDto) {
    return mapper(dbSession).selectForProjectUuidOrderedByName(projectDto.getUuid());
  }

  public void insert(DbSession dbSession, WebhookDto dto, @Nullable String projectKey, @Nullable String projectName) {
    mapper(dbSession).insert(dto.setCreatedAt(system2.now()).setUpdatedAt(system2.now()));
    auditPersister.addWebhook(dbSession, new WebhookNewValue(dto, projectKey, projectName));
  }

  public void update(DbSession dbSession, WebhookDto dto, @Nullable String projectKey, @Nullable String projectName) {
    mapper(dbSession).update(dto.setUpdatedAt(system2.now()));
    if (dto.getSecret() != null) {
      auditPersister.updateWebhookSecret(dbSession, new SecretNewValue("webhook_name", dto.getName()));
    }
    auditPersister.updateWebhook(dbSession, new WebhookNewValue(dto, projectKey, projectName));
  }

  public void delete(DbSession dbSession, String uuid, String webhookName) {
    int deletedRows = mapper(dbSession).delete(uuid);

    if (deletedRows > 0) {
      auditPersister.deleteWebhook(dbSession, new WebhookNewValue(uuid, webhookName));
    }
  }

  public void deleteByProject(DbSession dbSession, ProjectDto projectDto) {
    int deletedRows = mapper(dbSession).deleteForProjectUuid(projectDto.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteWebhook(dbSession, new WebhookNewValue(projectDto));
    }
  }

  private static WebhookMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(WebhookMapper.class);
  }

}
