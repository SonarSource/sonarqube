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

import org.sonar.batch.scan.ProjectAnalysisMode;

import org.sonar.batch.bootstrap.WSLoader;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.rule.ModuleQProfiles;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProjectRepositoriesLoaderTest {

  private DefaultProjectRepositoriesLoader loader;
  private WSLoader wsLoader;
  private ProjectAnalysisMode analysisMode;
  private ProjectReactor reactor;
  private AnalysisProperties taskProperties;

  @Before
  public void prepare() {
    wsLoader = mock(WSLoader.class);
    analysisMode = mock(ProjectAnalysisMode.class);
    loader = new DefaultProjectRepositoriesLoader(wsLoader, analysisMode);
    loader = spy(loader);
    when(wsLoader.loadString(anyString())).thenReturn("{}");
    taskProperties = new AnalysisProperties(Maps.<String, String>newHashMap(), "");
  }

  @Test
  public void passPreviewParameter() {
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    when(analysisMode.isPreview()).thenReturn(false);
    loader.load(reactor, taskProperties);
    verify(wsLoader).loadString("/batch/project?key=foo&preview=false");

    when(analysisMode.isPreview()).thenReturn(true);
    loader.load(reactor, taskProperties);
    verify(wsLoader).loadString("/batch/project?key=foo&preview=true");
  }

  @Test
  public void passAndEncodeProjectKeyParameter() {
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo bàr"));
    loader.load(reactor, taskProperties);
    verify(wsLoader).loadString("/batch/project?key=foo+b%C3%A0r&preview=false");
  }

  @Test
  public void passAndEncodeProfileParameter() {
    reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));
    taskProperties.properties().put(ModuleQProfiles.SONAR_PROFILE_PROP, "my-profile#2");
    loader.load(reactor, taskProperties);
    verify(wsLoader).loadString("/batch/project?key=foo&profile=my-profile%232&preview=false");
  }

}
