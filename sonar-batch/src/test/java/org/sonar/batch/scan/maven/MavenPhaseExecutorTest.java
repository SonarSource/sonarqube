/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.maven;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class MavenPhaseExecutorTest {

  @Test
  public void doNothingIfNoPhase() {
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    MavenPluginExecutor mavenPluginExecutor = mock(MavenPluginExecutor.class);
    MavenPhaseExecutor phaseExecutor = new MavenPhaseExecutor(fs, mavenPluginExecutor, new Settings(), mock(DatabaseSession.class));

    Project project = new Project("key");
    phaseExecutor.execute(project);

    verify(mavenPluginExecutor, never()).execute(eq(project), eq(fs), anyString());
  }

  @Test
  public void executePhase() {
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    MavenPluginExecutor mavenPluginExecutor = mock(MavenPluginExecutor.class);
    Settings settings = new Settings().setProperty(MavenPhaseExecutor.PROP_PHASE, "foo");
    MavenPhaseExecutor phaseExecutor = new MavenPhaseExecutor(fs, mavenPluginExecutor, settings, mock(DatabaseSession.class));

    Project project = mock(Project.class);
    phaseExecutor.execute(project);

    verify(mavenPluginExecutor).execute(project, fs, "foo");
  }
}
