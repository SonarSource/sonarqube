/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.cobertura.api;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaUtilsTest {
  @Test
  public void shouldGetReportPathFromProperty() throws URISyntaxException {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.resolvePath("foo")).thenReturn(getCoverageReport());

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(project.getProperty(CoberturaUtils.COBERTURA_REPORT_PATH_PROPERTY)).thenReturn("foo");

    File report = CoberturaUtils.getReport(project);
    verify(fileSystem).resolvePath("foo");
    assertNotNull(report);
  }

  @Test
  public void shouldGetReportPathFromPom() {
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");

    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);

    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(pom);
    when(project.getFileSystem()).thenReturn(fileSystem);

    CoberturaUtils.getReport(project);

    verify(fileSystem).resolvePath("overridden/dir");
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }
}
