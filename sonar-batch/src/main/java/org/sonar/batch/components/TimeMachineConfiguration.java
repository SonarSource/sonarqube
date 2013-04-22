/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.components;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;

import javax.persistence.Query;

import java.util.Date;
import java.util.List;

public class TimeMachineConfiguration implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TimeMachineConfiguration.class);

  private static final int NUMBER_OF_VARIATION_SNAPSHOTS = 5;
  private static final int CORE_TENDENCY_DEPTH_DEFAULT_VALUE = 30;

  private Project project;
  private final Settings settings;
  private List<PastSnapshot> projectPastSnapshots;
  private DatabaseSession session;

  public TimeMachineConfiguration(DatabaseSession session, Project project, Settings settings,
      PastSnapshotFinder pastSnapshotFinder) {
    this.session = session;
    this.project = project;
    this.settings = settings;
    initPastSnapshots(pastSnapshotFinder, getRootProject(project).getQualifier());
  }

  private Project getRootProject(Project project){
    if (!project.isRoot()) {
     return getRootProject(project.getRoot());
    }
    return project;
  }

  private void initPastSnapshots(PastSnapshotFinder pastSnapshotFinder, String rootQualifier) {
    Snapshot projectSnapshot = buildProjectSnapshot();

    projectPastSnapshots = Lists.newLinkedList();
    if (projectSnapshot != null) {
      for (int index = 1; index <= NUMBER_OF_VARIATION_SNAPSHOTS; index++) {
        PastSnapshot pastSnapshot = pastSnapshotFinder.find(projectSnapshot, rootQualifier, settings, index);
        if (pastSnapshot != null) {
          log(pastSnapshot);
          projectPastSnapshots.add(pastSnapshot);
        }
      }
    }
  }

  private Snapshot buildProjectSnapshot() {
    Query query = session
        .createNativeQuery("select p.id from projects p where p.kee=:resourceKey and p.qualifier<>:lib and p.enabled=:enabled");
    query.setParameter("resourceKey", project.getKey());
    query.setParameter("lib", Qualifiers.LIBRARY);
    query.setParameter("enabled", Boolean.TRUE);

    Snapshot snapshot = null;
    Number projectId = session.getSingleResult(query, null);
    if (projectId != null) {
      snapshot = new Snapshot();
      snapshot.setResourceId(projectId.intValue());
      snapshot.setCreatedAt(project.getAnalysisDate());
      snapshot.setBuildDate(new Date());
      snapshot.setVersion(project.getAnalysisVersion());
    }
    return snapshot;
  }

  private void log(PastSnapshot pastSnapshot) {
    String qualifier = pastSnapshot.getQualifier();
    // hack to avoid too many logs when the views plugin is installed
    if (StringUtils.equals(Qualifiers.VIEW, qualifier) || StringUtils.equals(Qualifiers.SUBVIEW, qualifier)) {
      LOG.debug(pastSnapshot.toString());
    } else {
      LOG.info(pastSnapshot.toString());
    }
  }

  public int getTendencyPeriodInDays() {
    return CORE_TENDENCY_DEPTH_DEFAULT_VALUE;
  }

  public List<PastSnapshot> getProjectPastSnapshots() {
    return projectPastSnapshots;
  }
}
