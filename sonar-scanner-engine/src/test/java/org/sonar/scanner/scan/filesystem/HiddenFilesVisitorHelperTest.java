/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonar.scanner.scan.ModuleConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HiddenFilesVisitorHelperTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static final SonarUserHome sonarUserHome = mock(SonarUserHome.class);
  private static final DefaultInputModule inputModule = mock(DefaultInputModule.class);

  private final ModuleConfiguration moduleConfig = mock(ModuleConfiguration.class);
  private final HiddenFilesProjectData hiddenFilesProjectData = spy(new HiddenFilesProjectData(sonarUserHome));
  private HiddenFilesVisitorHelper underTest;

  @BeforeClass
  public static void setUp() throws IOException {
    File userHomeFolder = temp.newFolder(".userhome");
    setAsHiddenOnWindows(userHomeFolder);
    when(sonarUserHome.getPath()).thenReturn(userHomeFolder.toPath());

    File workDir = temp.newFolder(".sonar");
    setAsHiddenOnWindows(workDir);
    when(inputModule.getWorkDir()).thenReturn(workDir.toPath());
  }

  @Before
  public void before() {
    hiddenFilesProjectData.clearHiddenFilesData();
    underTest = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));
  }

  @Test
  public void verifyDefaultOnConstruction() {
    assertThat(underTest.excludeHiddenFiles).isFalse();
    assertThat(underTest.rootHiddenDir).isNull();
  }

  @Test
  public void excludeHiddenFilesShouldBeSetToFalseFromConfigurationWhenNotConfigured() {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.empty());
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    assertThat(configuredVisitorHelper.excludeHiddenFiles).isFalse();
  }

  @Test
  public void excludeHiddenFilesShouldBeSetToFalseFromConfigurationWhenDisabled() {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(false));
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    assertThat(configuredVisitorHelper.excludeHiddenFiles).isFalse();
  }

  @Test
  public void excludeHiddenFilesShouldBeSetToTrueFromConfigurationWhenEnabled() {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(true));
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    assertThat(configuredVisitorHelper.excludeHiddenFiles).isTrue();
  }

  @Test
  public void shouldVisitHiddenDirectory() throws IOException {
    File hiddenDir = temp.newFolder(".hiddenVisited");
    setAsHiddenOnWindows(hiddenDir);

    boolean visitDir = underTest.shouldVisitDir(hiddenDir.toPath());

    assertThat(visitDir).isTrue();
    assertThat(underTest.insideHiddenDirectory()).isTrue();
    assertThat(underTest.rootHiddenDir).isEqualTo(hiddenDir.toPath());
    verify(underTest).enterHiddenDirectory(hiddenDir.toPath());
  }

  @Test
  public void shouldNotVisitHiddenDirectoryWhenHiddenFilesVisitIsExcluded() throws IOException {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(true));
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    File hidden = temp.newFolder(".hiddenNotVisited");
    setAsHiddenOnWindows(hidden);

    boolean visitDir = configuredVisitorHelper.shouldVisitDir(hidden.toPath());

    assertThat(visitDir).isFalse();
    assertThat(configuredVisitorHelper.insideHiddenDirectory()).isFalse();
    verify(configuredVisitorHelper, never()).enterHiddenDirectory(any());
  }

  @Test
  public void shouldVisitNonHiddenDirectoryWhenHiddenFilesVisitIsExcluded() throws IOException {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(true));
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    File nonHiddenFolder = temp.newFolder();

    boolean visitDir = configuredVisitorHelper.shouldVisitDir(nonHiddenFolder.toPath());

    assertThat(visitDir).isTrue();
    assertThat(configuredVisitorHelper.insideHiddenDirectory()).isFalse();
    verify(configuredVisitorHelper, never()).enterHiddenDirectory(any());
  }

  @Test
  public void shouldVisitNonHiddenDirectory() throws IOException {
    File nonHiddenFolder = temp.newFolder();

    boolean visitDir = underTest.shouldVisitDir(nonHiddenFolder.toPath());

    assertThat(visitDir).isTrue();
    assertThat(underTest.insideHiddenDirectory()).isFalse();
    verify(underTest, never()).enterHiddenDirectory(any());
    assertThat(underTest.excludeHiddenFiles).isFalse();
  }

  @Test
  public void shouldNotVisitModuleWorkDir() throws IOException {
    Path workingDirectory = inputModule.getWorkDir().toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    boolean visitDir = underTest.shouldVisitDir(workingDirectory);

    assertThat(visitDir).isFalse();
    assertThat(underTest.insideHiddenDirectory()).isFalse();
    verify(underTest, never()).enterHiddenDirectory(any());
  }

  @Test
  public void shouldNotVisitSonarUserHome() throws IOException {
    Path userHome = sonarUserHome.getPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    boolean visitDir = underTest.shouldVisitDir(userHome);

    assertThat(visitDir).isFalse();
    assertThat(underTest.insideHiddenDirectory()).isFalse();
    verify(underTest, never()).enterHiddenDirectory(any());
  }

  @Test
  public void hiddenFileShouldBeVisited() throws IOException {
    File hiddenFile = temp.newFile(".hiddenFileShouldBeVisited");
    setAsHiddenOnWindows(hiddenFile);

    assertThat(underTest.insideHiddenDirectory()).isFalse();
    boolean visitFile = underTest.shouldVisitFile(hiddenFile.toPath());

    assertThat(visitFile).isTrue();
    verify(hiddenFilesProjectData).markAsHiddenFile(hiddenFile.toPath(), inputModule);
  }

  @Test
  public void nonHiddenFileShouldBeVisitedInHiddenFolder() throws IOException {
    File hidden = temp.newFolder(".hiddenFolder");
    setAsHiddenOnWindows(hidden);

    File nonHiddenFile = temp.newFile();

    underTest.shouldVisitDir(hidden.toPath());
    assertThat(underTest.insideHiddenDirectory()).isTrue();

    boolean shouldVisitFile = underTest.shouldVisitFile(nonHiddenFile.toPath());

    assertThat(shouldVisitFile).isTrue();
    verify(hiddenFilesProjectData).markAsHiddenFile(nonHiddenFile.toPath(), inputModule);
  }

  @Test
  public void shouldNotSetAsRootHiddenDirectoryWhenAlreadyEnteredHiddenDirectory() throws IOException {
    File hidden = temp.newFolder(".outerHiddenFolder");
    File nestedHiddenFolder = temp.newFolder(".outerHiddenFolder", ".nestedHiddenFolder");
    setAsHiddenOnWindows(hidden);
    setAsHiddenOnWindows(nestedHiddenFolder);

    underTest.shouldVisitDir(hidden.toPath());
    assertThat(underTest.insideHiddenDirectory()).isTrue();

    boolean shouldVisitNestedDir = underTest.shouldVisitDir(nestedHiddenFolder.toPath());

    assertThat(shouldVisitNestedDir).isTrue();
    assertThat(underTest.rootHiddenDir).isEqualTo(hidden.toPath());
    verify(underTest).enterHiddenDirectory(nestedHiddenFolder.toPath());
  }

  @Test
  public void hiddenFileShouldNotBeVisitedWhenHiddenFileVisitExcluded() throws IOException {
    when(moduleConfig.getBoolean("sonar.scanner.excludeHiddenFiles")).thenReturn(Optional.of(true));
    HiddenFilesVisitorHelper configuredVisitorHelper = spy(new HiddenFilesVisitorHelper(hiddenFilesProjectData, inputModule, moduleConfig));

    File hiddenFile = temp.newFile(".hiddenFileNotVisited");
    setAsHiddenOnWindows(hiddenFile);

    assertThat(configuredVisitorHelper.insideHiddenDirectory()).isFalse();

    configuredVisitorHelper.shouldVisitFile(hiddenFile.toPath());
    boolean shouldVisitFile = configuredVisitorHelper.shouldVisitFile(hiddenFile.toPath());

    assertThat(shouldVisitFile).isFalse();
    verify(hiddenFilesProjectData, never()).markAsHiddenFile(hiddenFile.toPath(), inputModule);
  }

  @Test
  public void shouldCorrectlyExitHiddenFolderOnlyOnHiddenFolderThatEntered() throws IOException {
    File hiddenFolder = temp.newFolder(".hiddenRootFolder");
    setAsHiddenOnWindows(hiddenFolder);

    boolean shouldVisitDir = underTest.shouldVisitDir(hiddenFolder.toPath());

    assertThat(shouldVisitDir).isTrue();
    assertThat(underTest.insideHiddenDirectory()).isTrue();
    assertThat(underTest.rootHiddenDir).isEqualTo(hiddenFolder.toPath());
    verify(underTest).enterHiddenDirectory(hiddenFolder.toPath());

    File folder1 = temp.newFolder(".hiddenRootFolder", "myFolderExit");
    File folder2 = temp.newFolder("myFolderExit");
    File folder3 = temp.newFolder(".myFolderExit");
    setAsHiddenOnWindows(folder3);

    underTest.exitDirectory(folder1.toPath());
    underTest.exitDirectory(folder2.toPath());
    underTest.exitDirectory(folder3.toPath());

    assertThat(underTest.insideHiddenDirectory()).isTrue();
    assertThat(underTest.rootHiddenDir).isEqualTo(hiddenFolder.toPath());
    verify(underTest, never()).resetRootHiddenDir();

    underTest.exitDirectory(hiddenFolder.toPath());
    assertThat(underTest.insideHiddenDirectory()).isFalse();
    assertThat(underTest.rootHiddenDir).isNull();
    verify(underTest).resetRootHiddenDir();
  }

  @Test
  public void shouldNotInitiateResetRootDirWhenNotInHiddenDirectory() throws IOException {
    File hiddenFolder = temp.newFolder(".hiddenFolderNonRoot");
    setAsHiddenOnWindows(hiddenFolder);

    underTest.exitDirectory(hiddenFolder.toPath());

    verify(underTest, never()).resetRootHiddenDir();
  }

  @Test
  public void filesShouldBeCorrectlyMarkedAsHidden() throws IOException {
    File hiddenFolder = temp.newFolder(".hiddenFolderRoot");
    setAsHiddenOnWindows(hiddenFolder);

    File file1 = temp.newFile();
    File file2 = temp.newFile();
    File file3 = temp.newFile(".markedHiddenFile");
    setAsHiddenOnWindows(file3);
    File file4 = temp.newFile();
    File file5 = temp.newFile(".markedHiddenFile2");
    setAsHiddenOnWindows(file5);

    underTest.shouldVisitFile(file1.toPath());
    underTest.shouldVisitDir(hiddenFolder.toPath());
    underTest.shouldVisitFile(file2.toPath());
    underTest.shouldVisitFile(file3.toPath());
    underTest.exitDirectory(hiddenFolder.toPath());
    underTest.shouldVisitFile(file4.toPath());
    underTest.shouldVisitFile(file5.toPath());

    verify(hiddenFilesProjectData, never()).markAsHiddenFile(file1.toPath(), inputModule);
    verify(hiddenFilesProjectData).markAsHiddenFile(file2.toPath(), inputModule);
    verify(hiddenFilesProjectData).markAsHiddenFile(file3.toPath(), inputModule);
    verify(hiddenFilesProjectData, never()).markAsHiddenFile(file4.toPath(), inputModule);
    verify(hiddenFilesProjectData).markAsHiddenFile(file5.toPath(), inputModule);
  }

  private static void setAsHiddenOnWindows(File file) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      Files.setAttribute(file.toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
    }
  }
}
