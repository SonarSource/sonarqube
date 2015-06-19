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
package org.sonar.batch.scan.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectFileSystemAdapterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_wrap_module_file_system() {
    DefaultModuleFileSystem target = mock(DefaultModuleFileSystem.class, Mockito.RETURNS_SMART_NULLS);
    ProjectFileSystemAdapter adapter = new ProjectFileSystemAdapter(target, new Project("my-project"));

    assertThat(adapter.getBasedir()).isNotNull();
    verify(target).baseDir();

    assertThat(adapter.getSourceDirs()).isNotNull();
    verify(target).sourceDirs();

    assertThat(adapter.getTestDirs()).isNotNull();
    verify(target).testDirs();

    assertThat(adapter.getSourceCharset()).isNotNull();
    verify(target).sourceCharset();

    assertThat(adapter.getBuildDir()).isNotNull();
    verify(target).buildDir();
  }

  @Test
  public void should_create_default_build_dir() throws IOException {
    File workingDir = temp.newFile("work");
    DefaultModuleFileSystem target = mock(DefaultModuleFileSystem.class);
    when(target.workDir()).thenReturn(workingDir);
    ProjectFileSystemAdapter adapter = new ProjectFileSystemAdapter(target, new Project("my-project"));

    File buildDir = adapter.getBuildDir();
    assertThat(buildDir.getParentFile().getCanonicalPath()).isEqualTo(workingDir.getCanonicalPath());
  }
}
