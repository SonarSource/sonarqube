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
package org.sonar.batch.report;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.TempFolder;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.index.ResourceCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportPublisherTest extends AbstractDbUnitTestCase {

  private DefaultAnalysisMode mode;

  ResourceCache resourceCache = mock(ResourceCache.class);

  private ProjectReactor reactor;

  @Before
  public void setUp() {
    mode = mock(DefaultAnalysisMode.class);
    reactor = mock(ProjectReactor.class);
    when(reactor.getRoot()).thenReturn(ProjectDefinition.create().setKey("struts"));
  }

  @Test
  public void should_log_successful_analysis() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/");
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL, you can browse {}", "http://myserver/dashboard/index/struts");
    verify(logger).info("Note that you will be able to access the updated dashboard once the server has processed the submitted analysis report.");
  }

  @Test
  public void should_log_successful_preview_analysis() throws Exception {
    Settings settings = new Settings();
    when(mode.isPreview()).thenReturn(true);
    ReportPublisher job = new ReportPublisher(settings, mock(ServerClient.class), mock(Server.class), reactor, mode, mock(TempFolder.class), new ReportPublisherStep[0]);

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL");
  }

}
