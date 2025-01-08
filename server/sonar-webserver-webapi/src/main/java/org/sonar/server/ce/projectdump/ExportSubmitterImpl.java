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
package org.sonar.server.ce.projectdump;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.queue.CeTaskSubmit.Component.fromDto;

public class ExportSubmitterImpl implements ExportSubmitter {
  private final CeQueue ceQueue;
  private final DbClient dbClient;

  public ExportSubmitterImpl(CeQueue ceQueue, DbClient dbClient) {
    this.ceQueue = ceQueue;
    this.dbClient = dbClient;
  }

  @Override
  public CeTask submitProjectExport(String projectKey, @Nullable String submitterUuid) {
    requireNonNull(projectKey, "Project key can not be null");

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      Optional<ComponentDto> componentDto = dbClient.componentDao().selectByKey(dbSession, projectKey);
      checkArgument(project.isPresent(), "Project with key [%s] does not exist", projectKey);
      checkArgument(componentDto.isPresent(), "Component with key [%s] does not exist", projectKey);

      CeTaskSubmit submit = ceQueue.prepareSubmit()
        .setComponent(fromDto(componentDto.get().uuid(), project.get().getUuid()))
        .setType(CeTaskTypes.PROJECT_EXPORT)
        .setSubmitterUuid(submitterUuid)
        .setCharacteristics(emptyMap())
        .build();
      return ceQueue.submit(submit);
    }
  }
}
