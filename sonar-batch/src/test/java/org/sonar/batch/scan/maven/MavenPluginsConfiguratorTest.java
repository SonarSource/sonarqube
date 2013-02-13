/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.maven;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;
import org.sonar.batch.scan.maven.MavenPluginsConfigurator;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenPluginsConfiguratorTest {

  private MavenPluginsConfigurator newConfigurator(MavenPluginHandler... handlers) {
    BatchExtensionDictionnary selector = mock(BatchExtensionDictionnary.class);
    when(selector.selectMavenPluginHandlers(any(Project.class))).thenReturn(Arrays.asList(handlers));
    return new MavenPluginsConfigurator(selector);
  }

  @Test
  public void configureHandlers() {
    MavenPluginHandler handler1 = mock(MavenPluginHandler.class);
    when(handler1.getArtifactId()).thenReturn("myartifact1");

    MavenPluginHandler handler2 = mock(MavenPluginHandler.class);
    when(handler2.getArtifactId()).thenReturn("myartifact2");

    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom.xml");

    newConfigurator(handler1, handler2).execute(project);

    verify(handler1).configure(eq(project), argThat(new IsMavenPlugin("myartifact1")));
    verify(handler2).configure(eq(project), argThat(new IsMavenPlugin("myartifact2")));
  }

  private class IsMavenPlugin extends ArgumentMatcher<MavenPlugin> {
    private String artifactId;

    public IsMavenPlugin(String artifactId) {
      this.artifactId = artifactId;
    }

    @Override
    public boolean matches(Object o) {
      return artifactId.equals(((MavenPlugin) o).getPlugin().getArtifactId());
    }
  }
}
