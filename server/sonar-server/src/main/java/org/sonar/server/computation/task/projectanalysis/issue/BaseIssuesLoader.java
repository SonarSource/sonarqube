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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;

/**
 * Loads all the project open issues from database, including manual issues.
 */
public class BaseIssuesLoader {

  private final TreeRootHolder treeRootHolder;
  private final DbClient dbClient;

  public BaseIssuesLoader(TreeRootHolder treeRootHolder, DbClient dbClient) {
    this.treeRootHolder = treeRootHolder;
    this.dbClient = dbClient;
  }

  /**
   * Uuids of all the components that have open issues on this project.
   */
  public Set<String> loadUuidsOfComponentsWithOpenIssues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.issueDao().selectComponentUuidsOfOpenIssuesForProjectUuid(dbSession, treeRootHolder.getRoot().getUuid());
    }
  }
}
