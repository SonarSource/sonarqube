/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.squid;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class SquidSensorTest {
  @Test
  public void testGetBytecodeFiles() {
    ProjectClasspath projectClasspath = mock(ProjectClasspath.class);
    when(projectClasspath.getElements()).thenReturn(Arrays.asList(new File("classes")));
    SquidSensor sensor = new SquidSensor(null, null, projectClasspath, null);
    Configuration configuration = new BaseConfiguration();
    Project project = mock(Project.class);
    when(project.getConfiguration()).thenReturn(configuration);

    configuration.setProperty(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, true);
    assertThat(sensor.getBytecodeFiles(project).size(), is(0));

    configuration.setProperty(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, false);
    assertThat(sensor.getBytecodeFiles(project).size(), is(1));
    assertThat(sensor.getBytecodeFiles(project), sameInstance((Collection<File>) projectClasspath.getElements()));
  }

  @Test
  public void onlyForJava() {
    SquidSensor sensor = new SquidSensor(null, null, null, null);
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY).thenReturn("groovy");
    assertThat(sensor.shouldExecuteOnProject(project), is(true));
    assertThat(sensor.shouldExecuteOnProject(project), is(false));
    verify(project, times(2)).getLanguageKey();
    verifyNoMoreInteractions(project);
  }
}
