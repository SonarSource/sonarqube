/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.db.DbClient;

public class PurgeDatastoresStep implements ComputationStep {

  private final ProjectCleaner projectCleaner;
  private final DbClient dbClient;
  private final DbComponentsRefCache dbComponentsRefCache;
  private final ProjectSettingsRepository projectSettingsRepository;

  public PurgeDatastoresStep(DbClient dbClient, ProjectCleaner projectCleaner, DbComponentsRefCache dbComponentsRefCache, ProjectSettingsRepository projectSettingsRepository) {
    this.projectCleaner = projectCleaner;
    this.dbClient = dbClient;
    this.dbComponentsRefCache = dbComponentsRefCache;
    this.projectSettingsRepository = projectSettingsRepository;
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      DbComponentsRefCache.DbComponent project = dbComponentsRefCache.getByRef(context.getReportMetadata().getRootComponentRef());
      projectCleaner.purge(session, new IdUuidPair(project.getId(), project.getUuid()), projectSettingsRepository.getProjectSettings(project.getKey()));
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Purge datastores";
  }
}
