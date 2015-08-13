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
package org.sonar.batch.cache;

import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.home.cache.PersistentCache;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.analysis.AnalysisProperties;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import org.sonar.core.platform.ComponentContainer;
import org.junit.Test;

public class ProjectSyncContainerTest {
  private ComponentContainer createParentContainer() {
    PersistentCache cache = mock(PersistentCache.class);
    ServerClient server = mock(ServerClient.class);

    GlobalProperties globalProps = new GlobalProperties(new HashMap<String, String>());
    ComponentContainer parent = new ComponentContainer();
    parent.add(cache);
    parent.add(server);
    parent.add(globalProps);
    return parent;
  }

  public AnalysisProperties createProjectProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.branch", "branch");
    properties.put("sonar.projectKey", "my:project");
    properties.put("sonar.projectName", "My project");
    properties.put("sonar.projectVersion", "1.0");
    properties.put("sonar.sources", ".");
    properties.put("sonar.projectBaseDir", ".");
    return new AnalysisProperties(properties);
  }

  @Test
  public void testProjectKeyWithBranch() {
    ProjectSyncContainer container = new ProjectSyncContainer(createParentContainer(), createProjectProperties(), true);
    container.doBeforeStart();
    container.getPicoContainer().start();
    
    ProjectReactor projectReactor = container.getComponentByType(ProjectReactor.class);
    assertThat(projectReactor.getRoot().getKeyWithBranch()).isEqualTo("my:project:branch");
  }
}
