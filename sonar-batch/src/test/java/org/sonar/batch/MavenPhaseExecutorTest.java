/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MavenPhaseExecutorTest {

  @Test
  public void doNothingIfNoPhase() {
    MavenPluginExecutor mavenPluginExecutor = mock(MavenPluginExecutor.class);
    MavenPhaseExecutor phaseExecutor = new MavenPhaseExecutor(mavenPluginExecutor);


    Project project = new Project("key");
    phaseExecutor.execute(project, mock(SensorContext.class));

    verify(mavenPluginExecutor, never()).execute(eq(project), anyString());
  }

  @Test
  public void executePhase() {
    MavenPluginExecutor mavenPluginExecutor = mock(MavenPluginExecutor.class);
    MavenPhaseExecutor phaseExecutor = new MavenPhaseExecutor(mavenPluginExecutor);

    Project project = new Project("key");
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(MavenPhaseExecutor.PROP_PHASE, "myphase");
    project.setConfiguration(conf);

    phaseExecutor.execute(project, mock(SensorContext.class));

    verify(mavenPluginExecutor).execute(project, "myphase");
  }
}
