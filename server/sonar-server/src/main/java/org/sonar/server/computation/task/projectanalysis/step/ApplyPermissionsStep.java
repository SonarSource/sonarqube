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
package org.sonar.server.computation.task.projectanalysis.step;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DbIdsRepository;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.permission.index.AuthorizationIndexer;

import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Apply default permissions on new projects and index issues/authorization
 */
public class ApplyPermissionsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final AuthorizationIndexer indexer;
  private final PermissionRepository permissionRepository;
  private final TreeRootHolder treeRootHolder;

  public ApplyPermissionsStep(DbClient dbClient, DbIdsRepository dbIdsRepository, AuthorizationIndexer indexer, PermissionRepository permissionRepository,
                              TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.indexer = indexer;
    this.permissionRepository = permissionRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.reportMaxDepth(PROJECT).withViewsMaxDepth(VIEW), PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }

        @Override
        public void visitView(Component view) {
          execute(view);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void execute(Component project) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      long projectId = dbIdsRepository.getComponentId(project);
      if (hasNoPermissions(dbSession, projectId)) {
        permissionRepository.applyDefaultPermissionTemplate(dbSession, projectId);
        dbSession.commit();
        indexer.index();
      }
    }
  }

  private boolean hasNoPermissions(DbSession dbSession, long projectId) {
    return !dbClient.groupPermissionDao().hasRootComponentPermissions(dbSession, projectId) &&
      !dbClient.userPermissionDao().hasRootComponentPermissions(dbSession, projectId);
  }

  @Override
  public String getDescription() {
    return "Apply permissions";
  }

}
