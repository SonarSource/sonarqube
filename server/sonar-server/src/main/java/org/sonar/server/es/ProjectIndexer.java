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
package org.sonar.server.es;

/**
 * A {@link ProjectIndexer} populates an Elasticsearch index
 * containing project-related documents, for instance issues
 * or tests. This interface allows to quickly integrate new
 * indices in the lifecycle of projects.
 *
 * If the related index handles verification of authorization,
 * then the implementation of {@link ProjectIndexer} must
 * also implement {@link org.sonar.server.permission.index.NeedAuthorizationIndexer}
 */
public interface ProjectIndexer {

  enum Cause {
    PROJECT_CREATION, PROJECT_KEY_UPDATE, NEW_ANALYSIS, PROJECT_TAGS_UPDATE
  }

  /**
   * This method is called when a project must be (re-)indexed,
   * for example when project is created or when a new analysis
   * is being processed.
   * @param projectUuid non-null UUID of project
   * @param cause the reason why indexing is triggered. That
   *              allows some implementations to ignore
   *              re-indexing in some cases. For example
   *              there is no need to index measures when
   *              a project is being created because they
   *              are not computed yet.
   */
  void indexProject(String projectUuid, Cause cause);

  /**
   * This method is called when a project is deleted.
   * @param projectUuid non-null UUID of project
   */
  void deleteProject(String projectUuid);

}
