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
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.batch.DefaultTimeMachine;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.scan.filesystem.PreviousFileHashLoader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
  private final PreviousFileHashLoader fileHashLoader;
  private final MetricFinder metricFinder;
  private final DefaultTimeMachine defaultTimeMachine;

  public DefaultProjectReferentialsLoader(ServerClient serverClient, AnalysisMode analysisMode, PreviousFileHashLoader fileHashLoader, MetricFinder finder,
    DefaultTimeMachine defaultTimeMachine) {
    this.serverClient = serverClient;
    this.analysisMode = analysisMode;
    this.fileHashLoader = fileHashLoader;
    this.metricFinder = finder;
    this.defaultTimeMachine = defaultTimeMachine;
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

    Integer lastCommitsId = metricFinder.findByKey(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE.key()).getId();
    Integer revisionsId = metricFinder.findByKey(CoreMetrics.SCM_REVISIONS_BY_LINE.key()).getId();
    Integer authorsId = metricFinder.findByKey(CoreMetrics.SCM_AUTHORS_BY_LINE.key()).getId();
    for (Map.Entry<String, String> hashByPaths : fileHashLoader.hashByRelativePath().entrySet()) {
      String path = hashByPaths.getKey();
      String hash = hashByPaths.getValue();
      String lastCommits = null;
      String revisions = null;
      String authors = null;
      List<MeasureModel> measures = defaultTimeMachine.query(projectKey + ":" + path, lastCommitsId, revisionsId, authorsId);
      for (MeasureModel m : measures) {
        if (m.getMetricId() == lastCommitsId) {
          lastCommits = m.getData(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
        } else if (m.getMetricId() == revisionsId) {
          revisions = m.getData(CoreMetrics.SCM_REVISIONS_BY_LINE);
        }
        if (m.getMetricId() == authorsId) {
          authors = m.getData(CoreMetrics.SCM_AUTHORS_BY_LINE);
        }
      }
      ref.fileDataPerPath().put(path, new FileData(hash, lastCommits, revisions, authors));
    }
    return ref;
  }
}
