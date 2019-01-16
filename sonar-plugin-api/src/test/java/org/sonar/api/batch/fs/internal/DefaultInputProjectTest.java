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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultInputProjectTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testGetters() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("projectKey");
    def.setName("projectName");
    File baseDir = temp.newFolder();
    def.setBaseDir(baseDir);
    def.setDescription("desc");
    File workDir = temp.newFolder();
    def.setWorkDir(workDir);
    def.setSources("file1");
    def.setTests("test1");
    AbstractProjectOrModule project = new DefaultInputProject(def);

    assertThat(project.key()).isEqualTo("projectKey");
    assertThat(project.getName()).isEqualTo("projectName");
    assertThat(project.getOriginalName()).isEqualTo("projectName");
    assertThat(project.definition()).isEqualTo(def);
    assertThat(project.getBranch()).isNull();
    assertThat(project.getBaseDir()).isEqualTo(baseDir.toPath());
    assertThat(project.getKeyWithBranch()).isEqualTo("projectKey");
    assertThat(project.getDescription()).isEqualTo("desc");
    assertThat(project.getWorkDir()).isEqualTo(workDir.toPath());
    assertThat(project.getEncoding()).isEqualTo(Charset.defaultCharset());

    assertThat(project.properties()).hasSize(5);

    assertThat(project.isFile()).isFalse();
  }

  @Test
  public void testEncoding() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("projectKey");
    def.setName("projectName");
    File baseDir = temp.newFolder();
    def.setBaseDir(baseDir);
    def.setProjectVersion("version");
    def.setDescription("desc");
    File workDir = temp.newFolder();
    def.setWorkDir(workDir);
    def.setSources("file1");
    def.setProperty("sonar.sourceEncoding", "UTF-16");
    AbstractProjectOrModule project = new DefaultInputProject(def);

    assertThat(project.getEncoding()).isEqualTo(StandardCharsets.UTF_16);
  }

}
