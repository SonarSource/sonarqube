/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.fs.internal.fs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultInputModuleTest {

  private static final String FILE_1 = "file1";
  private static final String TEST_1 = "test1";
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void check_getters() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("moduleKey");
    File baseDir = temp.newFolder();
    Path src = baseDir.toPath().resolve(FILE_1);
    Files.createFile(src);
    Path test = baseDir.toPath().resolve(TEST_1);
    Files.createFile(test);
    def.setBaseDir(baseDir);
    File workDir = temp.newFolder();
    def.setWorkDir(workDir);
    def.setSources(FILE_1);
    def.setTests(TEST_1);
    DefaultInputModule module = new DefaultInputModule(def);

    assertThat(module.key()).isEqualTo("moduleKey");
    assertThat(module.definition()).isEqualTo(def);
    assertThat(module.getBaseDir()).isEqualTo(baseDir.toPath());
    assertThat(module.getWorkDir()).isEqualTo(workDir.toPath());
    assertThat(module.getEncoding()).isEqualTo(Charset.defaultCharset());
    assertThat(module.getSourceDirsOrFiles().get()).containsExactlyInAnyOrder(src);
    assertThat(module.getTestDirsOrFiles().get()).containsExactlyInAnyOrder(test);
    assertThat(module.getEncoding()).isEqualTo(Charset.defaultCharset());

    assertThat(module.isFile()).isFalse();
  }

  @Test
  public void no_sources() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("moduleKey");
    File baseDir = temp.newFolder();
    Path src = baseDir.toPath().resolve(FILE_1);
    Files.createFile(src);
    Path test = baseDir.toPath().resolve(TEST_1);
    Files.createFile(test);
    def.setBaseDir(baseDir);
    File workDir = temp.newFolder();
    def.setWorkDir(workDir);
    DefaultInputModule module = new DefaultInputModule(def);

    assertThat(module.key()).isEqualTo("moduleKey");
    assertThat(module.definition()).isEqualTo(def);
    assertThat(module.getBaseDir()).isEqualTo(baseDir.toPath());
    assertThat(module.getWorkDir()).isEqualTo(workDir.toPath());
    assertThat(module.getEncoding()).isEqualTo(Charset.defaultCharset());
    assertThat(module.getSourceDirsOrFiles()).isNotPresent();
    assertThat(module.getTestDirsOrFiles()).isNotPresent();
    assertThat(module.getEncoding()).isEqualTo(Charset.defaultCharset());

    assertThat(module.isFile()).isFalse();
  }

  @Test
  public void working_directory_should_be_hidden() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    File workDir = temp.newFolder(".sonar");
    def.setWorkDir(workDir);
    File baseDir = temp.newFolder();
    def.setBaseDir(baseDir);
    DefaultInputModule module = new DefaultInputModule(def);
    assertThat(workDir.isHidden()).isTrue();
  }

}
