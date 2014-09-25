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
package org.sonar.batch.referential;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotDataDto;

import javax.persistence.Query;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultProjectReferentialsLoader implements ProjectReferentialsLoader {

  private static final String BATCH_PROJECT_URL = "/batch/project";

  private static final List<Metric> METRICS = ImmutableList.<Metric>of(
    CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
    CoreMetrics.SCM_REVISIONS_BY_LINE,
    CoreMetrics.SCM_AUTHORS_BY_LINE);

  private final ServerClient serverClient;
  private final AnalysisMode analysisMode;
  private final SnapshotDataDao dao;
  private final DatabaseSession session;

  public DefaultProjectReferentialsLoader(DatabaseSession session, ServerClient serverClient, AnalysisMode analysisMode,
    SnapshotDataDao dao) {
    this.session = session;
    this.serverClient = serverClient;
    this.analysisMode = analysisMode;
    this.dao = dao;
  }

  @Override
  public ProjectReferentials load(ProjectReactor reactor, TaskProperties taskProperties) {
    String projectKey = reactor.getRoot().getKeyWithBranch();
    String url = BATCH_PROJECT_URL + "?key=" + projectKey;
    if (taskProperties.properties().containsKey(ModuleQProfiles.SONAR_PROFILE_PROP)) {
      try {
        url += "&profile=" + URLEncoder.encode(taskProperties.properties().get(ModuleQProfiles.SONAR_PROFILE_PROP), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Unable to encode URL", e);
      }
    }
    url += "&preview=" + analysisMode.isPreview();
    ProjectReferentials ref = ProjectReferentials.fromJson(serverClient.request(url));

    for (ProjectDefinition module : reactor.getProjects()) {

      for (Map.Entry<String, String> hashByPaths : hashByRelativePath(module.getKeyWithBranch()).entrySet()) {
        String path = hashByPaths.getKey();
        String hash = hashByPaths.getValue();
        String lastCommits = null;
        String revisions = null;
        String authors = null;
        List<Object[]> measuresByKey = query(projectKey + ":" + path, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, CoreMetrics.SCM_REVISIONS_BY_LINE_KEY,
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
        ref.addFileData(projectKey, path, new FileData(hash, lastCommits, revisions, authors));
      }
    }
    return ref;
  }

  public Map<String, String> hashByRelativePath(String projectKey) {
    Map<String, String> map = Maps.newHashMap();
    Collection<SnapshotDataDto> selectSnapshotData = dao.selectSnapshotDataByComponentKey(
      projectKey,
      Arrays.asList(SnapshotDataTypes.FILE_HASHES)
      );
    if (!selectSnapshotData.isEmpty()) {
      SnapshotDataDto snapshotDataDto = selectSnapshotData.iterator().next();
      String data = snapshotDataDto.getData();
      map = KeyValueFormat.parse(data);
    }
    return map;
  }

  public List<Object[]> query(String resourceKey, String... metricKeys) {
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
}
