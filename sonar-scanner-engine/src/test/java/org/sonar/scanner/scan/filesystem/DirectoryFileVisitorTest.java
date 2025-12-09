/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.ModuleConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DirectoryFileVisitorTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private final DefaultInputModule module = mock();
  private final ModuleConfiguration moduleConfiguration = mock();
  private final ModuleExclusionFilters moduleExclusionFilters = mock();
  private final InputModuleHierarchy inputModuleHierarchy = mock();
  private final InputFile.Type type = mock();
  private final SonarUserHome sonarUserHome = mock();
  private HiddenFilesProjectData hiddenFilesProjectData;

  @Before
  public void before() throws IOException {
    Path sonarUserHomePath = temp.newFolder().toPath();
    when(sonarUserHome.getPath()).thenReturn(sonarUserHomePath);
    File workDir = temp.newFolder();
    when(module.getWorkDir()).thenReturn(workDir.toPath());
    hiddenFilesProjectData = spy(new HiddenFilesProjectData(sonarUserHome));
  }

  @Test
  public void should_not_visit_hidden_file() throws IOException {
    when(moduleConfiguration.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(true));
    DirectoryFileVisitor.FileVisitAction action = mock(DirectoryFileVisitor.FileVisitAction.class);

    File hidden = temp.newFile(".hiddenNotVisited");
    setAsHiddenOnWindows(hidden);

    DirectoryFileVisitor underTest = new DirectoryFileVisitor(action, module, moduleConfiguration, moduleExclusionFilters, inputModuleHierarchy, type, hiddenFilesProjectData);
    underTest.visitFile(hidden.toPath(), Files.readAttributes(hidden.toPath(), BasicFileAttributes.class));

    verify(action, never()).execute(any(Path.class));
  }

  @Test
  public void should_visit_hidden_file() throws IOException {
    when(moduleConfiguration.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(false));
    DirectoryFileVisitor.FileVisitAction action = mock(DirectoryFileVisitor.FileVisitAction.class);

    File hidden = temp.newFile(".hiddenVisited");
    setAsHiddenOnWindows(hidden);

    DirectoryFileVisitor underTest = new DirectoryFileVisitor(action, module, moduleConfiguration, moduleExclusionFilters, inputModuleHierarchy, type, hiddenFilesProjectData);
    underTest.visitFile(hidden.toPath(), Files.readAttributes(hidden.toPath(), BasicFileAttributes.class));

    verify(action).execute(any(Path.class));
  }

  @Test
  public void test_visit_file_failed_generic_io_exception() throws IOException {
    DirectoryFileVisitor.FileVisitAction action = mock(DirectoryFileVisitor.FileVisitAction.class);

    File file = temp.newFile("failed");

    DirectoryFileVisitor underTest = new DirectoryFileVisitor(action, module, moduleConfiguration, moduleExclusionFilters, inputModuleHierarchy, type, hiddenFilesProjectData);
    assertThrows(IOException.class, () -> underTest.visitFileFailed(file.toPath(), new IOException()));
  }

  @Test
  public void test_visit_file_failed_file_system_loop_exception() throws IOException {
    DirectoryFileVisitor.FileVisitAction action = mock(DirectoryFileVisitor.FileVisitAction.class);

    File file = temp.newFile("symlink");

    DirectoryFileVisitor underTest = new DirectoryFileVisitor(action, module, moduleConfiguration, moduleExclusionFilters, inputModuleHierarchy, type, hiddenFilesProjectData);
    FileVisitResult result = underTest.visitFileFailed(file.toPath(), new FileSystemLoopException(file.getPath()));

    assertThat(result).isEqualTo(FileVisitResult.CONTINUE);
  }

  private static void setAsHiddenOnWindows(File file) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      Files.setAttribute(file.toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
    }
  }
}
