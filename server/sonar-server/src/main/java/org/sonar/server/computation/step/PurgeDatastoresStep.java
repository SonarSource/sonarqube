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
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.db.DbClient;

public class PurgeDatastoresStep implements ComputationStep {

  private final ProjectCleaner projectCleaner;
  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final TreeRootHolder treeRootHolder;
  private final SettingsRepository settingsRepository;

  public PurgeDatastoresStep(DbClient dbClient, ProjectCleaner projectCleaner, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder,
    SettingsRepository settingsRepository) {
    this.projectCleaner = projectCleaner;
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.treeRootHolder = treeRootHolder;
    this.settingsRepository = settingsRepository;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      Component project = treeRootHolder.getRoot();
      projectCleaner.purge(session, new IdUuidPair(dbIdsRepository.getComponentId(project), project.getUuid()), settingsRepository.getSettings(project));
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
