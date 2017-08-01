/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.startup;

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;

/**
 * Display a log for each project that hasn't been analysed since SonarQube 4.2 (column DEPRECATED_KEE is null)
 *
 * Should be removed after next LTS
 *
 * @since 5.2
 */
public class DisplayLogOnDeprecatedProjects implements Startable {

  private static final Logger LOG = Loggers.get(DisplayLogOnDeprecatedProjects.class);

  private static final String TEMPLATE_KEY = "DisplayLogOnDeprecatedProjects";

  private final DbClient dbClient;

  public DisplayLogOnDeprecatedProjects(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!hasAlreadyBeenExecuted(dbSession)) {
        return;
      }
      displayLogOnDeprecatedProjectKeys(dbSession);
      registerTasks(dbSession);
      dbSession.commit();
    }
  }

  private void displayLogOnDeprecatedProjectKeys(DbSession session) {
    boolean hasDetectDeprecatedProjects = false;
    for (ComponentDto project : dbClient.componentDao().selectProjects(session)) {
      if (project.deprecatedKey() == null) {
        if (!hasDetectDeprecatedProjects) {
          LOG.warn("We detected that the following projects have not been analysed on a SonarQube version greater than 4.2 (included):");
          hasDetectDeprecatedProjects = true;
        }
        LOG.warn(" - {}", project.getDbKey());
      }
    }
    if (hasDetectDeprecatedProjects) {
      LOG.warn("As a consequence, some features of the Web UI will be broken for them, and any new analysis will consider all issues as new issues.");
    }
  }

  private boolean hasAlreadyBeenExecuted(DbSession session) {
    return dbClient.loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.ONE_SHOT_TASK_TYPE, TEMPLATE_KEY, session) == 0;
  }

  private void registerTasks(DbSession session) {
    dbClient.loadedTemplateDao().insert(new LoadedTemplateDto(TEMPLATE_KEY, LoadedTemplateDto.ONE_SHOT_TASK_TYPE), session);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
