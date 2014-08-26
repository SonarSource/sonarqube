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
import com.google.common.io.InputSupplier;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.rule.ModuleQProfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.anyInt;
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
    loader = new DefaultProjectReferentialsLoader(serverClient, analysisMode);
    when(serverClient.doRequest(anyString(), anyInt())).thenReturn(new InputSupplier<InputStream>() {

      @Override
      public InputStream getInput() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
      }
    });
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    taskProperties = new TaskProperties(Maps.<String, String>newHashMap(), "");
  }

  @Test
  public void passPreviewParameter() {
    when(analysisMode.isPreview()).thenReturn(false);
    loader.load(reactor, taskProperties);
    verify(serverClient).doRequest("/batch/project?key=foo&preview=false", null);

    when(analysisMode.isPreview()).thenReturn(true);
    loader.load(reactor, taskProperties);
    verify(serverClient).doRequest("/batch/project?key=foo&preview=true", null);
  }

  @Test
  public void passProfileParameter() {
    taskProperties.properties().put(ModuleQProfiles.SONAR_PROFILE_PROP, "my-profile");
    loader.load(reactor, taskProperties);
    verify(serverClient).doRequest("/batch/project?key=foo&profile=my-profile&preview=false", null);
  }

}
