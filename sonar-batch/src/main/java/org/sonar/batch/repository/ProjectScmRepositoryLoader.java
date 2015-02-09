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
package org.sonar.batch.repository;

import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;

import javax.annotation.CheckForNull;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * Waiting for SCM measure copy to be handled in computation stack we need to get previous measures from DB
 */
@RequiresDB
public class ProjectScmRepositoryLoader implements BatchComponent {

  private final DatabaseSession session;
  private final ProjectReactor reactor;
  private final ProjectRepositories ref;

  public ProjectScmRepositoryLoader(DatabaseSession session, ProjectReactor reactor, ProjectRepositories ref) {
    this.session = session;
    this.reactor = reactor;
    this.ref = ref;
  }

  public void complete() {
    for (ProjectDefinition module : reactor.getProjects()) {

      for (Entry<String, FileData> fileDataByPaths : ref.fileDataByPath(module.getKeyWithBranch()).entrySet()) {
        String path = fileDataByPaths.getKey();
        FileData fileData = fileDataByPaths.getValue();
        String lastCommits = null;
        String revisions = null;
        String authors = null;
        List<Object[]> measuresByKey = query(module.getKeyWithBranch() + ":" + path, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, CoreMetrics.SCM_REVISIONS_BY_LINE_KEY,
          CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
        for (Object[] measureByKey : measuresByKey) {
          if (measureByKey[0].equals(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY)) {
            lastCommits = ((MeasureModel) measureByKey[1]).getData(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
          } else if (measureByKey[0].equals(CoreMetrics.SCM_REVISIONS_BY_LINE_KEY)) {
            revisions = ((MeasureModel) measureByKey[1]).getData(CoreMetrics.SCM_REVISIONS_BY_LINE);
          } else if (measureByKey[0].equals(CoreMetrics.SCM_AUTHORS_BY_LINE_KEY)) {
            authors = ((MeasureModel) measureByKey[1]).getData(CoreMetrics.SCM_AUTHORS_BY_LINE);
          }
        }
        ref.addFileData(module.getKeyWithBranch(), path, new FileData(fileData.hash(), authors == null, lastCommits, revisions, authors));
      }
    }
    ref.setLastAnalysisDate(lastSnapshotCreationDate(reactor.getRoot().getKeyWithBranch()));
  }

  private List<Object[]> query(String resourceKey, String... metricKeys) {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> params = Maps.newHashMap();

    sb.append("SELECT met.key, m");
    sb.append(" FROM ")
      .append(MeasureModel.class.getSimpleName())
      .append(" m, ")
      .append(Metric.class.getSimpleName())
      .append(" met, ")
      .append(ResourceModel.class.getSimpleName())
      .append(" r, ")
      .append(Snapshot.class.getSimpleName())
      .append(" s WHERE met.id=m.metricId AND m.snapshotId=s.id AND s.resourceId=r.id AND r.key=:kee AND s.status=:status AND s.qualifier<>:lib");
    params.put("kee", resourceKey);
    params.put("status", Snapshot.STATUS_PROCESSED);
    params.put("lib", Qualifiers.LIBRARY);

    sb.append(" AND m.characteristicId IS NULL");
    sb.append(" AND m.personId IS NULL");
    sb.append(" AND m.ruleId IS NULL AND m.rulePriority IS NULL");
    if (metricKeys.length > 0) {
      sb.append(" AND met.key IN (:metricKeys) ");
      params.put("metricKeys", Arrays.asList(metricKeys));
    }
    sb.append(" AND s.last=true ");
    sb.append(" ORDER BY s.createdAt ");

    Query jpaQuery = session.createQuery(sb.toString());

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      jpaQuery.setParameter(entry.getKey(), entry.getValue());
    }
    return jpaQuery.getResultList();
  }

  @CheckForNull
  Date lastSnapshotCreationDate(String resourceKey) {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> params = Maps.newHashMap();

    sb.append("SELECT s.buildDate");
    sb.append(" FROM ")
      .append(ResourceModel.class.getSimpleName())
      .append(" r, ")
      .append(Snapshot.class.getSimpleName())
      .append(" s WHERE s.resourceId=r.id AND r.key=:kee AND s.status=:status AND s.qualifier<>:lib");
    params.put("kee", resourceKey);
    params.put("status", Snapshot.STATUS_PROCESSED);
    params.put("lib", Qualifiers.LIBRARY);

    sb.append(" AND s.last=true ");

    Query jpaQuery = session.createQuery(sb.toString());

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      jpaQuery.setParameter(entry.getKey(), entry.getValue());
    }
    try {
      return longToDate((Long) jpaQuery.getSingleResult());
    } catch (NoResultException e) {
      return null;
    }
  }
}
