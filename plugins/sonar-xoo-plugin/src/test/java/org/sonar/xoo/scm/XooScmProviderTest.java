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
package org.sonar.xoo.scm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class XooScmProviderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private XooScmProvider underTest;
  private XooBlameCommand blameCommand;
  private XooIgnoreCommand ignoreCommand;

  @Before
  public void setUp() {
    blameCommand = mock(XooBlameCommand.class);
    ignoreCommand = mock(XooIgnoreCommand.class);
    underTest = new XooScmProvider(blameCommand, ignoreCommand);
  }

  @Test
  public void key_returnsXoo() {
    assertThat(underTest.key()).isEqualTo("xoo");
  }

  @Test
  public void supports_withXooMarkerFile_returnsTrue() throws IOException {
    File baseDir = temp.newFolder();
    new File(baseDir, ".xoo").createNewFile();

    assertThat(underTest.supports(baseDir)).isTrue();
  }

  @Test
  public void supports_withoutXooMarkerFile_returnsFalse() throws IOException {
    File baseDir = temp.newFolder();

    assertThat(underTest.supports(baseDir)).isFalse();
  }

  @Test
  public void blameCommand_returnsBlameCommand() {
    assertThat(underTest.blameCommand()).isEqualTo(blameCommand);
  }

  @Test
  public void ignoreCommand_returnsIgnoreCommand() {
    assertThat(underTest.ignoreCommand()).isEqualTo(ignoreCommand);
  }

  @Test
  public void revisionId_returnsFakeSha1() throws IOException {
    Path path = temp.newFile().toPath();
    assertThat(underTest.revisionId(path)).isEqualTo("fakeSha1FromXoo");
  }

  @Test
  public void branchChangedFiles_withNoScmFiles_returnsNull() throws IOException {
    Path baseDir = temp.newFolder().toPath();

    Set<Path> result = underTest.branchChangedFiles("main", baseDir);

    assertThat(result).isNull();
  }

  @Test
  public void branchChangedFiles_withScmFilesAndChangedLines_returnsChangedFiles() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);
    Files.writeString(scmFile, "author,date,rev,true\nauthor,date,rev,false", StandardCharsets.UTF_8);

    Set<Path> result = underTest.branchChangedFiles("main", baseDir);

    assertThat(result).containsExactly(sourceFile);
  }

  @Test
  public void branchChangedFiles_withScmFilesButNoChangedLines_returnsEmptySet() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);
    Files.writeString(scmFile, "author,date,rev,false\nauthor,date,rev,false", StandardCharsets.UTF_8);

    Set<Path> result = underTest.branchChangedFiles("main", baseDir);

    assertThat(result).isEmpty();
  }

  @Test
  public void branchChangedFiles_withScmFileButNoSourceFile_skipsFile() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(scmFile, "author,date,rev,true", StandardCharsets.UTF_8);

    Set<Path> result = underTest.branchChangedFiles("main", baseDir);

    assertThat(result).isEmpty();
  }

  @Test
  public void branchChangedLines_withChangedLines_returnsLineNumbers() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);
    Files.writeString(scmFile, "author,date,rev,true\nauthor,date,rev,false\nauthor,date,rev,true", StandardCharsets.UTF_8);

    Map<Path, Set<Integer>> result = underTest.branchChangedLines("main", baseDir, Set.of(sourceFile));

    assertThat(result).containsKey(sourceFile);
    assertThat(result.get(sourceFile)).containsExactlyInAnyOrder(1, 3);
  }

  @Test
  public void branchChangedLines_withNoChangedLines_returnsNull() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);
    Files.writeString(scmFile, "author,date,rev,false", StandardCharsets.UTF_8);

    Map<Path, Set<Integer>> result = underTest.branchChangedLines("main", baseDir, Set.of(sourceFile));

    assertThat(result).isNull();
  }

  @Test
  public void branchChangedLines_withMissingScmFile_returnsNull() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);

    Map<Path, Set<Integer>> result = underTest.branchChangedLines("main", baseDir, Set.of(sourceFile));

    assertThat(result).isNull();
  }

  @Test
  public void branchChangedFiles_withScmFileLackingNewLineFlag_returnsNull() throws IOException {
    Path baseDir = temp.newFolder().toPath();
    Path sourceFile = baseDir.resolve("test.xoo");
    Path scmFile = baseDir.resolve("test.xoo.scm");
    
    Files.writeString(sourceFile, "content", StandardCharsets.UTF_8);
    Files.writeString(scmFile, "author,date,rev", StandardCharsets.UTF_8);

    Set<Path> result = underTest.branchChangedFiles("main", baseDir);

    assertThat(result).isNull();
  }
}
