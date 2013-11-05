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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;

import javax.persistence.Query;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

public class TimeMachineConfiguration implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TimeMachineConfiguration.class);

  private final DatabaseSession session;
  private Project project;
  private final PeriodsDefinition periodsDefinition;

  private List<Period> periods;
  private List<PastSnapshot> modulePastSnapshots;

  public TimeMachineConfiguration(DatabaseSession session, Project project, PeriodsDefinition periodsDefinition) {
    this.session = session;
    this.project = project;
    this.periodsDefinition = periodsDefinition;
    initModulePastSnapshots();
  }

  private void initModulePastSnapshots() {
    periods = newLinkedList();
    modulePastSnapshots = newLinkedList();
    for (PastSnapshot projectPastSnapshot : periodsDefinition.projectPastSnapshots()) {
      Date snapshotDate = getSnapshotDate(projectPastSnapshot.getProjectSnapshot());

      PastSnapshot pastSnapshot = new PastSnapshot(projectPastSnapshot.getMode(), projectPastSnapshot.getTargetDate(), projectPastSnapshot.getProjectSnapshot());
      pastSnapshot.setIndex(projectPastSnapshot.getIndex());
      pastSnapshot.setMode(projectPastSnapshot.getMode());
      pastSnapshot.setModeParameter(projectPastSnapshot.getModeParameter());
      modulePastSnapshots.add(pastSnapshot);
      periods.add(new Period(projectPastSnapshot.getIndex(), projectPastSnapshot.getTargetDate(), snapshotDate));
      log(pastSnapshot);
    }
  }

  private Date getSnapshotDate(Snapshot projectSnapshot) {
    Query query = session
      .createNativeQuery("select s.created_at from snapshots s where s.project_id=:projectId and (s.root_snapshot_id=:rootSnapshotId or s.id=:rootSnapshotId) and s.islast=:islast");
    query.setParameter("projectId", project.getId());
    query.setParameter("rootSnapshotId", projectSnapshot.getId());
    query.setParameter("islast", Boolean.TRUE);

    Date date = session.getSingleResult(query, null);
    return date;
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

  public List<Period> periods() {
    return periods;
  }

  /**
   * Only used by VariationDecorator
   */
  public List<PastSnapshot> modulePastSnapshots() {
    return modulePastSnapshots;
  }
}
