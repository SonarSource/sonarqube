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
package org.sonar.batch.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.deprecated.components.PeriodsDefinition;

import javax.annotation.CheckForNull;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.sonar.api.utils.DateUtils.longToDate;

@RequiresDB
@BatchSide
public class TimeMachineConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(TimeMachineConfiguration.class);

  private final DatabaseSession session;
  private final PeriodsDefinition periodsDefinition;

  private List<Period> periods;
  private List<PastSnapshot> modulePastSnapshots;

  public TimeMachineConfiguration(DatabaseSession session, PeriodsDefinition periodsDefinition) {
    this.session = session;
    this.periodsDefinition = periodsDefinition;
    initModulePastSnapshots();
  }

  private void initModulePastSnapshots() {
    periods = newLinkedList();
    modulePastSnapshots = newLinkedList();
    for (PastSnapshot projectPastSnapshot : periodsDefinition.getRootProjectPastSnapshots()) {
      Snapshot snapshot = findSnapshot(projectPastSnapshot.getProjectSnapshot());

      PastSnapshot pastSnapshot = projectPastSnapshot.clonePastSnapshot();
      modulePastSnapshots.add(pastSnapshot);
      // When no snapshot is found, date of the period is null
      periods.add(new Period(pastSnapshot.getIndex(), snapshot != null ? longToDate(snapshot.getCreatedAtMs()) : null));
      LOG.info(pastSnapshot.toString());
    }
  }

  /**
   * Only used to get the real date of the snapshot on the current period.
   * The date is used to calculate new_violations measures
   */
  @CheckForNull
  private Snapshot findSnapshot(Snapshot projectSnapshot) {
    String hql = "from " + Snapshot.class.getSimpleName() + " where resourceId=:resourceId and (rootId=:rootSnapshotId or id=:rootSnapshotId)";
    List<Snapshot> snapshots = session.createQuery(hql)
      .setParameter("resourceId", projectSnapshot.getResourceId())
      .setParameter("rootSnapshotId", projectSnapshot.getId())
      .setMaxResults(1)
      .getResultList();
    return snapshots.isEmpty() ? null : snapshots.get(0);
  }

  public List<Period> periods() {
    return periods;
  }

  public List<PastSnapshot> getProjectPastSnapshots() {
    return modulePastSnapshots;
  }
}
