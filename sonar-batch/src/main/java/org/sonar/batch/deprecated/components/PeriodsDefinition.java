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

package org.sonar.batch.deprecated.components;

import org.sonar.api.BatchSide;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;

import javax.persistence.Query;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.sonar.api.utils.DateUtils.dateToLong;

@BatchSide
public class PeriodsDefinition {

  private static final int NUMBER_OF_VARIATION_SNAPSHOTS = 5;

  private DatabaseSession session;

  private ProjectTree projectTree;
  private final Settings settings;

  private List<PastSnapshot> projectPastSnapshots;

  public PeriodsDefinition(DatabaseSession session, ProjectTree projectTree, Settings settings,
    PastSnapshotFinder pastSnapshotFinder) {
    this.session = session;
    this.projectTree = projectTree;
    this.settings = settings;
    initPastSnapshots(pastSnapshotFinder, projectTree.getRootProject().getQualifier());
  }

  private void initPastSnapshots(PastSnapshotFinder pastSnapshotFinder, String rootQualifier) {
    Snapshot projectSnapshot = buildProjectSnapshot();
    projectPastSnapshots = newLinkedList();
    if (projectSnapshot != null) {
      for (int index = 1; index <= NUMBER_OF_VARIATION_SNAPSHOTS; index++) {
        PastSnapshot pastSnapshot = pastSnapshotFinder.find(projectSnapshot, rootQualifier, settings, index);
        // SONAR-4700 Add a past snapshot only if it exists
        if (pastSnapshot != null && pastSnapshot.getProjectSnapshot() != null) {
          projectPastSnapshots.add(pastSnapshot);
        }
      }
    }
  }

  private Snapshot buildProjectSnapshot() {
    Query query = session
      .createNativeQuery("select p.id from projects p where p.kee=:resourceKey and p.qualifier<>:lib and p.enabled=:enabled");
    query.setParameter("resourceKey", projectTree.getRootProject().getKey());
    query.setParameter("lib", Qualifiers.LIBRARY);
    query.setParameter("enabled", Boolean.TRUE);

    Snapshot snapshot = null;
    Number projectId = session.getSingleResult(query, null);
    if (projectId != null) {
      snapshot = new Snapshot();
      snapshot.setResourceId(projectId.intValue());
      snapshot.setCreatedAtMs(dateToLong(projectTree.getRootProject().getAnalysisDate()));
      snapshot.setBuildDateMs(System.currentTimeMillis());
      snapshot.setVersion(projectTree.getRootProject().getAnalysisVersion());
    }
    return snapshot;
  }

  /**
   * @return past snapshots of root project
   */
  public List<PastSnapshot> getRootProjectPastSnapshots() {
    return projectPastSnapshots;
  }

}
