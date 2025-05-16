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
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.SonarUserHome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HiddenFilesProjectDataTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static final SonarUserHome sonarUserHome = mock(SonarUserHome.class);
  private final DefaultInputModule inputModule = mock(DefaultInputModule.class);
  private final DefaultInputModule secondInputModule = mock(DefaultInputModule.class);
  private HiddenFilesProjectData underTest;

  @BeforeClass
  public static void setUp() throws IOException {
    File userHomeFolder = temp.newFolder(".userhome");
    setAsHiddenOnWindows(userHomeFolder);
    when(sonarUserHome.getPath()).thenReturn(userHomeFolder.toPath());
  }

  @Before
  public void before() {
    underTest = spy(new HiddenFilesProjectData(sonarUserHome));
  }

  @Test
  public void shouldContainNoMarkedHiddenFileOnConstruction() {
    assertThat(underTest.hiddenFilesByModule).isEmpty();
  }

  @Test
  public void shouldMarkWithCorrectAssociatedInputModule() {
    Path myFile = Path.of("myFile");
    Path myFile2 = Path.of("myFile2");
    underTest.markAsHiddenFile(myFile, inputModule);
    underTest.markAsHiddenFile(myFile2, inputModule);

    assertThat(underTest.hiddenFilesByModule).hasSize(1);
    assertThat(underTest.isMarkedAsHiddenFile(myFile, inputModule)).isTrue();
    assertThat(underTest.isMarkedAsHiddenFile(myFile2, inputModule)).isTrue();
    assertThat(underTest.isMarkedAsHiddenFile(myFile, secondInputModule)).isFalse();
    assertThat(underTest.isMarkedAsHiddenFile(myFile2, secondInputModule)).isFalse();
  }

  @Test
  public void shouldMarkWithCorrectAssociatedInputModuleForTwoDifferentModules() {
    Path myFile = Path.of("myFile");
    Path myFile2 = Path.of("myFile2");
    underTest.markAsHiddenFile(myFile, inputModule);
    underTest.markAsHiddenFile(myFile2, secondInputModule);

    assertThat(underTest.hiddenFilesByModule).hasSize(2);
    assertThat(underTest.isMarkedAsHiddenFile(myFile, inputModule)).isTrue();
    assertThat(underTest.isMarkedAsHiddenFile(myFile2, inputModule)).isFalse();
    assertThat(underTest.isMarkedAsHiddenFile(myFile, secondInputModule)).isFalse();
    assertThat(underTest.isMarkedAsHiddenFile(myFile2, secondInputModule)).isTrue();
  }

  @Test
  public void shouldNotShowAsHiddenFileWhenInputModuleIsNotExistingInData() {
    Path myFile = Path.of("myFile");
    Path notMarkedFile = Path.of("notMarkedFile");
    underTest.markAsHiddenFile(myFile, inputModule);

    assertThat(underTest.hiddenFilesByModule).isNotEmpty();
    assertThat(underTest.isMarkedAsHiddenFile(notMarkedFile, secondInputModule)).isFalse();
  }

  @Test
  public void shouldClearMap() {
    Path myFile = Path.of("myFile");
    Path myFile2 = Path.of("myFile2");
    underTest.markAsHiddenFile(myFile, inputModule);
    underTest.markAsHiddenFile(myFile2, secondInputModule);

    assertThat(underTest.hiddenFilesByModule).hasSize(2);

    underTest.clearHiddenFilesData();
    assertThat(underTest.hiddenFilesByModule).isEmpty();
  }

  @Test
  public void shouldNotFailOnUserPathResolving() throws IOException {
    Path expectedPath = sonarUserHome.getPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    assertThat(underTest.getCachedSonarUserHomePath()).isEqualTo(expectedPath);
  }

  private static void setAsHiddenOnWindows(File file) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      Files.setAttribute(file.toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
    }
  }
}
