/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.monitoring;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.Enumeration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

//@Execution(SAME_THREAD) for JUnit5
public class ServerMonitoringMetricsTest {

  @Before
  public void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  public void creatingClassShouldAddMetricsToRegistry() {
    assertThat(sizeOfDefaultRegistry()).isNotPositive();

    new ServerMonitoringMetrics();

    assertThat(sizeOfDefaultRegistry()).isPositive();
  }

  @Test
  public void setters_setGreenStatusForMetricsInTheMetricsRegistry() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setGithubStatusToGreen();
    metrics.setGitlabStatusToGreen();
    metrics.setAzureStatusToGreen();
    metrics.setBitbucketStatusToGreen();
    metrics.setComputeEngineStatusToGreen();
    metrics.setElasticSearchStatusToGreen();

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_github_status")).isPositive();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_gitlab_status")).isPositive();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_bitbucket_status")).isPositive();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_azuredevops_status")).isPositive();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_compute_engine_status")).isPositive();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_elasticsearch_status")).isPositive();
  }

  @Test
  public void setters_setRedStatusForMetricsInTheMetricsRegistry() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setGithubStatusToRed();
    metrics.setGitlabStatusToRed();
    metrics.setAzureStatusToRed();
    metrics.setBitbucketStatusToRed();
    metrics.setComputeEngineStatusToRed();
    metrics.setElasticSearchStatusToRed();

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_github_status")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_gitlab_status")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_bitbucket_status")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_integration_azuredevops_status")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_compute_engine_status")).isZero();
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_health_elasticsearch_status")).isZero();
  }

  @Test
  public void setters_setNumberOfPendingTasks() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setNumberOfPendingTasks(10);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_compute_engine_pending_tasks_total"))
      .isEqualTo(10);
  }

  @Test
  public void setters_setLicenseRelatedMetrics() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setLicenseDayUntilExpire(10);
    metrics.setLinesOfCodeRemaining(20);
    metrics.setLinesOfCodeAnalyzed(30);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_license_days_before_expiration_total"))
      .isEqualTo(10);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_license_number_of_lines_remaining_total"))
      .isEqualTo(20);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_license_number_of_lines_analyzed_total"))
      .isEqualTo(30);
  }

  @Test
  public void setters_setWebUptimeMetric() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setWebUptimeMinutes(10);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_web_uptime_minutes"))
      .isEqualTo(10);
  }

  @Test
  public void setters_setNumberOfConnectedSonarLintClients() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();

    metrics.setNumberOfConnectedSonarLintClients(5);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_number_of_connected_sonarlint_clients"))
      .isEqualTo(5);
  }

  @Test
  public void setters_setElasticsearchMetricsWithLabels() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();
    String[] labelNames = {"node_name"};
    String[] labelValues = {"node_1"};

    metrics.setElasticSearchDiskSpaceFreeBytes(labelValues[0], 30);
    metrics.setElasticSearchDiskSpaceTotalBytes(labelValues[0], 30);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_elasticsearch_disk_space_total_bytes",
      labelNames, labelValues))
      .isEqualTo(30);
    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_elasticsearch_disk_space_free_bytes",
      labelNames, labelValues))
      .isEqualTo(30);
  }

  @Test
  public void observeComputeEngineTaskDurationTest() {
    ServerMonitoringMetrics metrics = new ServerMonitoringMetrics();
    String[] labelNames = {"task_type", "project_key"};
    String[] labelValues = {"REPORT", "projectKey"};

    metrics.observeComputeEngineTaskDuration(10, labelValues[0], labelValues[1]);

    assertThat(CollectorRegistry.defaultRegistry.getSampleValue("sonarqube_compute_engine_tasks_running_duration_seconds_sum",
      labelNames, labelValues)).isEqualTo(10);
  }

  private int sizeOfDefaultRegistry() {
    Enumeration<Collector.MetricFamilySamples> metrics = CollectorRegistry.defaultRegistry.metricFamilySamples();
    return Collections.list(metrics).size();
  }
}
