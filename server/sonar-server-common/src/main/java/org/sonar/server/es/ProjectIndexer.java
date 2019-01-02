/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Collection;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;

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
public interface ProjectIndexer extends ResilientIndexer {

  enum Cause {
    PROJECT_CREATION,
    PROJECT_DELETION,
    PROJECT_KEY_UPDATE,
    PROJECT_TAGS_UPDATE,
    PERMISSION_CHANGE,
    MEASURE_CHANGE
  }

  /**
   * This method is called when an analysis must be indexed.
   *
   * @param branchUuid non-null UUID of branch in table "projects". It can reference
   *                   a non-main branch
   */
  void indexOnAnalysis(String branchUuid);

  Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause);
}
