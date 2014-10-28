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
package org.sonar.batch.phases;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateStatusJobTest extends AbstractDbUnitTestCase {

  private AnalysisMode mode;

  @Before
  public void setUp() {
    mode = mock(AnalysisMode.class);
  }

  @Test
  public void should_log_successful_analysis() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/");
    Project project = new Project("struts");
    UpdateStatusJob job = new UpdateStatusJob(settings, mock(ServerClient.class), project, mock(Snapshot.class), mode);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL, you can browse {}", "http://myserver/dashboard/index/struts");
    verify(logger).info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
  }

  @Test
  public void should_log_successful_preview_analysis() throws Exception {
    Settings settings = new Settings();
    when(mode.isPreview()).thenReturn(true);
    Project project = new Project("struts");
    UpdateStatusJob job = new UpdateStatusJob(settings, mock(ServerClient.class), project, mock(Snapshot.class), mode);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_publish_results_for_regular_analysis() throws Exception {
    Settings settings = new Settings();
    Project project = new Project("struts");
    ServerClient serverClient = mock(ServerClient.class);
    UpdateStatusJob job = new UpdateStatusJob(settings, serverClient, project, mock(Snapshot.class), mode);

    job.uploadReport();
    verify(serverClient).request(contains("/batch_bootstrap/evict"), eq("POST"));
    verify(serverClient).request(contains("/batch/upload_report"), eq("POST"), eq(true), eq(0));
  }

  @Test
  public void should_not_publish_results_for_preview_analysis() throws Exception {
    Settings settings = new Settings();
    when(mode.isPreview()).thenReturn(true);
    Project project = new Project("struts");
    ServerClient serverClient = mock(ServerClient.class);
    UpdateStatusJob job = new UpdateStatusJob(settings, serverClient, project, mock(Snapshot.class), mode);

    job.uploadReport();
    verify(serverClient, never()).request(anyString());
  }
}
