/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;

@ServerSide
public class PermissionService {

  private final DbClient dbClient;
  private final PermissionRepository permissionRepository;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public PermissionService(DbClient dbClient, PermissionRepository permissionRepository, IssueAuthorizationIndexer issueAuthorizationIndexer, UserSession userSession,
    ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.permissionRepository = permissionRepository;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public List<String> globalPermissions() {
    return GlobalPermissions.ALL;
  }

  public void applyDefaultPermissionTemplate(String componentKey) {
    DbSession session = dbClient.openSession(false);
    try {
      applyDefaultPermissionTemplate(session, componentKey);
    } finally {
      session.close();
    }
    indexProjectPermissions();
  }

  public void applyDefaultPermissionTemplate(DbSession session, String componentKey) {
    ComponentDto component = componentFinder.getByKey(session, componentKey);
    ResourceDto provisioned = dbClient.resourceDao().selectProvisionedProject(session, componentKey);
    if (provisioned == null) {
      checkProjectAdminUserByComponentKey(userSession, componentKey);
    } else {
      userSession.checkPermission(GlobalPermissions.PROVISIONING);
    }
    permissionRepository.applyDefaultPermissionTemplate(session, component);
    session.commit();
    indexProjectPermissions();
  }

  /**
   * Important - this method checks caller permissions
   */
  public void applyPermissionTemplate(ApplyPermissionTemplateQuery query) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      applyPermissionTemplate(dbSession, query);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public void applyPermissionTemplate(DbSession dbSession, ApplyPermissionTemplateQuery query) {
    if (query.getComponentKeys().size() == 1) {
      checkProjectAdminUserByComponentKey(userSession, query.getComponentKeys().get(0));
    } else {
      checkGlobalAdminUser(userSession);
    }

    // TODO apply permission templates in on query instead of on on each project
    for (String componentKey : query.getComponentKeys()) {
      ComponentDto component = componentFinder.getByKey(dbSession, componentKey);
      permissionRepository.applyPermissionTemplate(dbSession, query.getTemplateUuid(), component.getId());
    }
    dbSession.commit();

    indexProjectPermissions();
  }

  private void indexProjectPermissions() {
    issueAuthorizationIndexer.index();
  }
}
