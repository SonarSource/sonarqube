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
package org.sonar.scanner.scan;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultInputModuleHierarchyTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputModuleHierarchy moduleHierarchy;

  @Test
  public void test() throws IOException {
    DefaultInputModule root = new DefaultInputModule(ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputModule mod1 = new DefaultInputModule(ProjectDefinition.create().setKey("mod1").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputModule mod2 = new DefaultInputModule(ProjectDefinition.create().setKey("mod2").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputModule mod3 = new DefaultInputModule(ProjectDefinition.create().setKey("mod3").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputModule mod4 = new DefaultInputModule(ProjectDefinition.create().setKey("mod4").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));

    Map<DefaultInputModule, DefaultInputModule> parents = new HashMap<>();

    parents.put(mod1, root);
    parents.put(mod2, mod1);
    parents.put(mod3, root);
    parents.put(mod4, root);

    moduleHierarchy = new DefaultInputModuleHierarchy(root, parents);

    assertThat(moduleHierarchy.children(root)).containsOnly(mod1, mod3, mod4);
    assertThat(moduleHierarchy.children(mod4)).isEmpty();
    assertThat(moduleHierarchy.children(mod1)).containsOnly(mod2);

    assertThat(moduleHierarchy.parent(mod4)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(mod2)).isEqualTo(mod1);
    assertThat(moduleHierarchy.parent(mod1)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(root)).isNull();

    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }

  @Test
  public void testOnlyRoot() throws IOException {
    DefaultInputModule root = new DefaultInputModule(ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    moduleHierarchy = new DefaultInputModuleHierarchy(root);

    assertThat(moduleHierarchy.children(root)).isEmpty();
    assertThat(moduleHierarchy.parent(root)).isNull();
    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }

  @Test
  public void testRelativePathToRoot() throws IOException {
    File rootBaseDir = temp.newFolder();
    File mod1BaseDir = new File(rootBaseDir, "mod1");
    File mod2BaseDir = new File(rootBaseDir, "mod2");
    FileUtils.forceMkdir(mod1BaseDir);
    FileUtils.forceMkdir(mod2BaseDir);
    DefaultInputModule root = new DefaultInputModule(ProjectDefinition.create().setKey("root")
      .setBaseDir(rootBaseDir).setWorkDir(rootBaseDir));
    DefaultInputModule mod1 = new DefaultInputModule(ProjectDefinition.create().setKey("mod1")
      .setBaseDir(mod1BaseDir).setWorkDir(temp.newFolder()));
    DefaultInputModule mod2 = new DefaultInputModule(ProjectDefinition.create().setKey("mod2")
      .setBaseDir(mod2BaseDir).setWorkDir(temp.newFolder()));
    DefaultInputModule mod3 = new DefaultInputModule(ProjectDefinition.create().setKey("mod2")
      .setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));

    Map<DefaultInputModule, DefaultInputModule> parents = new HashMap<>();

    parents.put(mod1, root);
    parents.put(mod2, mod1);
    parents.put(mod3, mod1);

    moduleHierarchy = new DefaultInputModuleHierarchy(root, parents);

    assertThat(moduleHierarchy.relativePathToRoot(root)).isEqualTo("");
    assertThat(moduleHierarchy.relativePathToRoot(mod1)).isEqualTo("mod1");
    assertThat(moduleHierarchy.relativePathToRoot(mod2)).isEqualTo("mod2");
    assertThat(moduleHierarchy.relativePathToRoot(mod3)).isNull();
  }
}
