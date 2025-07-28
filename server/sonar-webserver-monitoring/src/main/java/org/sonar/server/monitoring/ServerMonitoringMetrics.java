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

import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.sonar.api.server.ServerSide;

@ServerSide
public class ServerMonitoringMetrics {

  public static final int UP_STATUS = 1;
  public static final int DOWN_STATUS = 0;
  private final Gauge githubHealthIntegrationStatus;
  private final Gauge gitlabHealthIntegrationStatus;
  private final Gauge bitbucketHealthIntegrationStatus;
  private final Gauge azureHealthIntegrationStatus;
  private final Gauge computeEngineGauge;
  private final Gauge elasticsearchGauge;

  private final Gauge cePendingTasksTotal;
  private final Summary ceTasksRunningDuration;
  private final Summary ceSystemTasksRunningDuration;
  private final Gauge elasticsearchDiskSpaceFreeBytesGauge;
  private final Gauge elasticSearchDiskSpaceTotalBytes;

  private final Gauge licenseDaysBeforeExpiration;
  private final Gauge linesOfCodeRemaining;
  private final Gauge linesOfCodeAnalyzed;

  private final Gauge webUptimeMinutes;

  private final Gauge numberOfConnectedSonarLintClients;

  public ServerMonitoringMetrics() {
    githubHealthIntegrationStatus = Gauge.build()
      .name("sonarqube_health_integration_github_status")
      .help("Tells whether SonarQube instance has configured GitHub integration and its status is green. 1 for green, 0 otherwise .")
      .register();

    gitlabHealthIntegrationStatus = Gauge.build()
      .name("sonarqube_health_integration_gitlab_status")
      .help("Tells whether SonarQube instance has configured GitLab integration and its status is green. 1 for green, 0 otherwise .")
      .register();

    bitbucketHealthIntegrationStatus = Gauge.build()
      .name("sonarqube_health_integration_bitbucket_status")
      .help("Tells whether SonarQube instance has configured BitBucket integration and its status is green. 1 for green, 0 otherwise .")
      .register();

    azureHealthIntegrationStatus = Gauge.build()
      .name("sonarqube_health_integration_azuredevops_status")
      .help("Tells whether SonarQube instance has configured Azure integration and its status is green. 1 for green, 0 otherwise .")
      .register();

    cePendingTasksTotal = Gauge.build()
      .name("sonarqube_compute_engine_pending_tasks_total")
      .help("Number of tasks at given point of time that were pending in the Compute Engine queue [SHARED, same value for every SonarQube instance]")
      .register();

    ceTasksRunningDuration = Summary.build()
      .name("sonarqube_compute_engine_tasks_running_duration_seconds")
      .help("Compute engine task running time in seconds")
      .labelNames("task_type", "project_key")
      .register();

    ceSystemTasksRunningDuration = Summary.build()
      .name("sonarqube_compute_engine_system_tasks_running_duration_seconds")
      .help("Compute engine system task running time in seconds")
      .labelNames("task_type")
      .register();

    computeEngineGauge = Gauge.build()
      .name("sonarqube_health_compute_engine_status")
      .help("Tells whether Compute Engine is up (healthy, ready to take tasks) or down. 1 for up, 0 for down")
      .register();

    elasticsearchGauge = Gauge.build()
      .name("sonarqube_health_elasticsearch_status")
      .help("Tells whether Elasticsearch is up or down. 1 for Up, 0 for down")
      .register();

    licenseDaysBeforeExpiration = Gauge.build()
      .name("sonarqube_license_days_before_expiration_total")
      .help("Days until the SonarQube license will expire.")
      .register();

    linesOfCodeRemaining = Gauge.build()
      .name("sonarqube_license_number_of_lines_remaining_total")
      .help("Number of lines remaining until the limit for the current license is hit.")
      .register();

    linesOfCodeAnalyzed = Gauge.build()
      .name("sonarqube_license_number_of_lines_analyzed_total")
      .help("Number of lines analyzed.")
      .register();

    elasticsearchDiskSpaceFreeBytesGauge = Gauge.build()
      .name("sonarqube_elasticsearch_disk_space_free_bytes")
      .help("Space left on device")
      .labelNames("node_name")
      .register();

    elasticSearchDiskSpaceTotalBytes = Gauge.build()
      .name("sonarqube_elasticsearch_disk_space_total_bytes")
      .help("Total disk space on the device")
      .labelNames("node_name")
      .register();

    webUptimeMinutes = Gauge.build()
      .name("sonarqube_web_uptime_minutes")
      .help("Number of minutes for how long the SonarQube instance is running")
      .register();

    numberOfConnectedSonarLintClients = Gauge.build()
      .name("sonarqube_number_of_connected_sonarlint_clients")
      .help("Number of connected SonarLint clients")
      .register();

  }

  public void setGithubStatusToGreen() {
    githubHealthIntegrationStatus.set(UP_STATUS);
  }

  public void setGithubStatusToRed() {
    githubHealthIntegrationStatus.set(DOWN_STATUS);
  }

  public void setGitlabStatusToGreen() {
    gitlabHealthIntegrationStatus.set(UP_STATUS);
  }

  public void setGitlabStatusToRed() {
    gitlabHealthIntegrationStatus.set(DOWN_STATUS);
  }

  public void setAzureStatusToGreen() {
    azureHealthIntegrationStatus.set(UP_STATUS);
  }

  public void setAzureStatusToRed() {
    azureHealthIntegrationStatus.set(DOWN_STATUS);
  }

  public void setBitbucketStatusToGreen() {
    bitbucketHealthIntegrationStatus.set(UP_STATUS);
  }

  public void setBitbucketStatusToRed() {
    bitbucketHealthIntegrationStatus.set(DOWN_STATUS);
  }

  public void setNumberOfPendingTasks(int numberOfPendingTasks) {
    cePendingTasksTotal.set(numberOfPendingTasks);
  }

  public void observeComputeEngineTaskDuration(long durationInSeconds, String taskType, String label) {
    ceTasksRunningDuration.labels(taskType, label).observe(durationInSeconds);
  }

  public void observeComputeEngineSystemTaskDuration(long durationInSeconds, String taskType) {
    ceSystemTasksRunningDuration.labels(taskType).observe(durationInSeconds);
  }

  public void setComputeEngineStatusToGreen() {
    computeEngineGauge.set(UP_STATUS);
  }

  public void setComputeEngineStatusToRed() {
    computeEngineGauge.set(DOWN_STATUS);
  }

  public void setElasticSearchStatusToGreen() {
    elasticsearchGauge.set(UP_STATUS);
  }

  public void setElasticSearchStatusToRed() {
    elasticsearchGauge.set(DOWN_STATUS);
  }

  public void setLicenseDayUntilExpire(long days) {
    licenseDaysBeforeExpiration.set(days);
  }

  public void setLinesOfCodeRemaining(long loc) {
    linesOfCodeRemaining.set(loc);
  }

  public void setLinesOfCodeAnalyzed(long loc) {
    linesOfCodeAnalyzed.set(loc);
  }

  public void setElasticSearchDiskSpaceFreeBytes(String name, long diskAvailableBytes) {
    elasticsearchDiskSpaceFreeBytesGauge.labels(name).set(diskAvailableBytes);
  }

  public void setElasticSearchDiskSpaceTotalBytes(String name, long diskTotalBytes) {
    elasticSearchDiskSpaceTotalBytes.labels(name).set(diskTotalBytes);
  }

  public void setWebUptimeMinutes(long minutes) {
    webUptimeMinutes.set(minutes);
  }

  public void setNumberOfConnectedSonarLintClients(long noOfClients) {
    numberOfConnectedSonarLintClients.set(noOfClients);
  }
}
