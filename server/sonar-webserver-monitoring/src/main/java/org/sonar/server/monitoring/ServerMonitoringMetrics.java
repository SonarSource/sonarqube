/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

  private final Gauge githubConfigOk;
  private final Gauge gitlabConfigOk;
  private final Gauge bitbucketConfigOk;
  private final Gauge azureConfigOk;

  private final Gauge cePendingTasksTotal;
  private final Summary ceTasksRunningDuration;

  public ServerMonitoringMetrics() {
    githubConfigOk = Gauge.build()
      .name("github_config_ok")
      .help("Tells whether SonarQube instance has configured GitHub integration and its status is green. 0 for green, 1 otherwise .")
      .register();

    gitlabConfigOk = Gauge.build()
      .name("gitlab_config_ok")
      .help("Tells whether SonarQube instance has configured GitLab integration and its status is green. 0 for green, 1 otherwise .")
      .register();

    bitbucketConfigOk = Gauge.build()
      .name("bitbucket_config_ok")
      .help("Tells whether SonarQube instance has configured BitBucket integration and its status is green. 0 for green, 1 otherwise .")
      .register();

    azureConfigOk = Gauge.build()
      .name("azure_config_ok")
      .help("Tells whether SonarQube instance has configured Azure integration and its status is green. 0 for green, 1 otherwise .")
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
  }

  public void setGithubStatusToGreen() {
    githubConfigOk.set(0);
  }

  public void setGithubStatusToRed() {
    githubConfigOk.set(1);
  }

  public void setGitlabStatusToGreen() {
    gitlabConfigOk.set(0);
  }

  public void setGitlabStatusToRed() {
    gitlabConfigOk.set(1);
  }

  public void setAzureStatusToGreen() {
    azureConfigOk.set(0);
  }

  public void setAzureStatusToRed() {
    azureConfigOk.set(1);
  }

  public void setBitbucketStatusToGreen() {
    bitbucketConfigOk.set(0);
  }

  public void setBitbucketStatusToRed() {
    bitbucketConfigOk.set(1);
  }

  public void setNumberOfPendingTasks(int numberOfPendingTasks) {
    cePendingTasksTotal.set(numberOfPendingTasks);
  }

  public void observeComputeEngineTaskDuration(long durationInSeconds, String taskType, String projectKey) {
    ceTasksRunningDuration.labels(taskType, projectKey).observe(durationInSeconds);
  }

}
