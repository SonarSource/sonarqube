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
package org.sonar.api.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.api.Plugins;
import org.sonar.api.resources.Project;

public class AbstractCoverageExtensionTest {

  @Test
  public void defaultPluginIsCobertura() {
    Plugins plugins = mock(Plugins.class);
    when(plugins.getPluginKeyByExtension(FakeCoverageSensor.class)).thenReturn("cobertura");

    Project project = mock(Project.class);
    when(project.getConfiguration()).thenReturn(new PropertiesConfiguration());

    assertThat(new FakeCoverageSensor(plugins).isSelectedPlugin(project), is(true));
  }

  @Test
  public void doNotExecuteIfNotSelectedPlugin() {
    Plugins plugins = mock(Plugins.class);
    when(plugins.getPluginKeyByExtension(FakeCoverageSensor.class)).thenReturn("fake");

    Project project = mock(Project.class);
    PropertiesConfiguration config = new PropertiesConfiguration();
    when(project.getConfiguration()).thenReturn(config);
    config.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "cobertura");

    assertThat(new FakeCoverageSensor(plugins).isSelectedPlugin(project), is(false));
  }

  @Test
  public void doNotExecuteIfStaticAnalysis() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    FakeCoverageSensor sensor = new FakeCoverageSensor(null);

    assertThat(sensor.shouldExecuteOnProject(project), is(false));
  }

  protected static class FakeCoverageSensor extends AbstractCoverageExtension {
    public FakeCoverageSensor(Plugins plugins) {
      super(plugins);
    }
  }
}
