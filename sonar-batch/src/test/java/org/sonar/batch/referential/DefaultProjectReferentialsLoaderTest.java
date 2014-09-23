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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.batch.DefaultTimeMachine;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.scan.filesystem.PreviousFileHashLoader;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProjectReferentialsLoaderTest {

  private DefaultProjectReferentialsLoader loader;
  private ServerClient serverClient;
  private AnalysisMode analysisMode;
  private ProjectReactor reactor;
  private TaskProperties taskProperties;

  @Before
  public void prepare() {
    serverClient = mock(ServerClient.class);
    analysisMode = mock(AnalysisMode.class);
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(anyString())).thenReturn(new Metric().setId(1));
    loader = new DefaultProjectReferentialsLoader(serverClient, analysisMode, mock(PreviousFileHashLoader.class), metricFinder, mock(DefaultTimeMachine.class));
    when(serverClient.request(anyString())).thenReturn("");
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    taskProperties = new TaskProperties(Maps.<String, String>newHashMap(), "");
  }

  @Test
  public void passPreviewParameter() {
    when(analysisMode.isPreview()).thenReturn(false);
    loader.load(reactor, taskProperties);
    verify(serverClient).request("/batch/project?key=foo&preview=false");

    when(analysisMode.isPreview()).thenReturn(true);
    loader.load(reactor, taskProperties);
    verify(serverClient).request("/batch/project?key=foo&preview=true");
  }

  @Test
  public void passProfileParameter() {
    taskProperties.properties().put(ModuleQProfiles.SONAR_PROFILE_PROP, "my-profile");
    loader.load(reactor, taskProperties);
    verify(serverClient).request("/batch/project?key=foo&profile=my-profile&preview=false");
  }

}
