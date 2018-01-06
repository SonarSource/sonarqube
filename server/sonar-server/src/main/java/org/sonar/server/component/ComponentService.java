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
package org.sonar.server.component;

import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.core.component.ComponentKeys.isValidModuleKey;
import static org.sonar.db.component.ComponentKeyUpdaterDao.checkIsProjectOrModule;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class ComponentService {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectIndexers projectIndexers;

  public ComponentService(DbClient dbClient, UserSession userSession, ProjectIndexers projectIndexers) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
  }

  // TODO should be moved to UpdateKeyAction
  public void updateKey(DbSession dbSession, ComponentDto projectOrModule, String newKey) {
    userSession.checkComponentPermission(UserRole.ADMIN, projectOrModule);
    checkIsProjectOrModule(projectOrModule);
    checkProjectOrModuleKeyFormat(newKey);
    dbClient.componentKeyUpdaterDao().updateKey(dbSession, projectOrModule.uuid(), newKey);
    projectIndexers.commitAndIndex(dbSession, singletonList(projectOrModule), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  // TODO should be moved to BulkUpdateKeyAction
  public void bulkUpdateKey(DbSession dbSession, ComponentDto projectOrModule, String stringToReplace, String replacementString) {
    dbClient.componentKeyUpdaterDao().bulkUpdateKey(dbSession, projectOrModule.uuid(), stringToReplace, replacementString);
    projectIndexers.commitAndIndex(dbSession, singletonList(projectOrModule), ProjectIndexer.Cause.PROJECT_KEY_UPDATE);
  }

  private static void checkProjectOrModuleKeyFormat(String key) {
    checkRequest(isValidModuleKey(key), "Malformed key for '%s'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", key);
  }

}
