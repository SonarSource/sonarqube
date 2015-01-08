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
package org.sonar.batch.design;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourcePersister;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenDependenciesSensorTest {

  private MavenDependenciesSensor sensor;
  private Settings settings;
  private SonarIndex sonarIndex;
  private SensorContext sensorContext;

  @Before
  public void prepare() {
    settings = new Settings();
    sonarIndex = mock(SonarIndex.class);
    sensor = new MavenDependenciesSensor(settings, sonarIndex, mock(ResourcePersister.class));
    sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(any(Library.class))).thenAnswer(new Answer<Library>() {
      public Library answer(InvocationOnMock invocation) throws Throwable {
        return (invocation.getArguments()[0] instanceof Library) ? (Library) invocation.getArguments()[0] : null;
      }
    });

  }

  @Test
  public void testDependenciesProvidedAsProperties() {
    settings
      .setProperty(
        "sonar.maven.projectDependencies",
        "[{\"k\":\"antlr:antlr\",\"v\":\"2.7.2\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"commons-beanutils:commons-beanutils\",\"v\":\"1.7.0\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"commons-chain:commons-chain\",\"v\":\"1.1\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"commons-digester:commons-digester\",\"v\":\"1.8\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"commons-fileupload:commons-fileupload\",\"v\":\"1.1.1\",\"s\":\"compile\",\"d\":[{\"k\":\"commons-io:commons-io\",\"v\":\"1.1\",\"s\":\"compile\",\"d\":[]}]},"
          + "{\"k\":\"commons-logging:commons-logging\",\"v\":\"1.0.4\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"commons-validator:commons-validator\",\"v\":\"1.3.1\",\"s\":\"compile\",\"d\":[]},"
          + "{\"k\":\"javax.servlet:servlet-api\",\"v\":\"2.3\",\"s\":\"provided\",\"d\":[]},"
          + "{\"k\":\"junit:junit\",\"v\":\"3.8.1\",\"s\":\"test\",\"d\":[]},"
          + "{\"k\":\"oro:oro\",\"v\":\"2.0.8\",\"s\":\"compile\",\"d\":[]}]");

    Project project = new Project("foo");
    sensor.analyse(project, sensorContext);

    Library antlr = new Library("antlr:antlr", "2.7.2");
    verify(sonarIndex).addResource(eq(antlr));
    Library commonsFU = new Library("commons-fileupload:commons-fileupload", "1.1.1");
    verify(sonarIndex).addResource(eq(commonsFU));
    Library commonsIo = new Library("commons-io:commons-io", "1.1");
    verify(sonarIndex).addResource(eq(commonsIo));
    Library junit = new Library("junit:junit", "3.8.1");
    verify(sonarIndex).addResource(eq(junit));

    verify(sensorContext).saveDependency(new Dependency(project, antlr).setUsage("compile").setWeight(1));
    verify(sensorContext).saveDependency(new Dependency(commonsFU, commonsIo).setUsage("compile").setWeight(1));
    verify(sensorContext).saveDependency(new Dependency(project, junit).setUsage("test").setWeight(1));
  }

}
