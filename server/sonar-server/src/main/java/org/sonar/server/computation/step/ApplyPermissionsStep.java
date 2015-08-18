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

import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Apply default permissions on new projects and index issues/authorization
 */
public class ApplyPermissionsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final IssueAuthorizationIndexer indexer;
  private final PermissionRepository permissionRepository;
  private final TreeRootHolder treeRootHolder;

  public ApplyPermissionsStep(DbClient dbClient, DbIdsRepository dbIdsRepository, IssueAuthorizationIndexer indexer,
    PermissionRepository permissionRepository, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.indexer = indexer;
    this.permissionRepository = permissionRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void execute(Component project) {
    DbSession session = dbClient.openSession(false);
    try {
      long projectId = dbIdsRepository.getComponentId(project);
      if (dbClient.roleDao().countComponentPermissions(session, projectId) == 0) {
        permissionRepository.grantDefaultRoles(session, projectId, Qualifiers.PROJECT);
        session.commit();
      }
      // As batch is still apply permission on project, indexing of issue authorization must always been done
      indexer.index();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Apply project permissions";
  }

}
