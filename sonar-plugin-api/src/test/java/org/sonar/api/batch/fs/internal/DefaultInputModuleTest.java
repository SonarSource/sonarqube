/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultInputModuleTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testGetters() throws IOException {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("projectKey");
    def.setName("projectName");
    File baseDir = temp.newFolder();
    def.setBaseDir(baseDir);
    def.setVersion("version");
    def.setDescription("desc");
    File workDir = temp.newFolder();
    def.setWorkDir(workDir);
    def.setSources("file1");
    def.setTests("test1");
    DefaultInputModule module = new DefaultInputModule(def);

    assertThat(module.key()).isEqualTo("projectKey");
    assertThat(module.getName()).isEqualTo("projectName");
    assertThat(module.getOriginalName()).isEqualTo("projectName");
    assertThat(module.definition()).isEqualTo(def);
    assertThat(module.getBranch()).isNull();
    assertThat(module.getBaseDir()).isEqualTo(baseDir.toPath());
    assertThat(module.getKeyWithBranch()).isEqualTo("projectKey");
    assertThat(module.getVersion()).isEqualTo("version");
    assertThat(module.getOriginalVersion()).isEqualTo("version");
    assertThat(module.getDescription()).isEqualTo("desc");
    assertThat(module.getWorkDir()).isEqualTo(workDir.toPath());

    assertThat(module.properties()).hasSize(6);

    assertThat(module.isFile()).isFalse();
  }

}
