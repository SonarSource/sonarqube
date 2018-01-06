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
package org.sonar.scanner.scan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    moduleHierarchy = new DefaultInputModuleHierarchy(parents);

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
  public void testParentChild() throws IOException {
    DefaultInputModule root = new DefaultInputModule(ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputModule mod1 = new DefaultInputModule(ProjectDefinition.create().setKey("mod1").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    moduleHierarchy = new DefaultInputModuleHierarchy(root, mod1);

    assertThat(moduleHierarchy.children(root)).containsOnly(mod1);
    assertThat(moduleHierarchy.children(mod1)).isEmpty();

    assertThat(moduleHierarchy.parent(mod1)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(root)).isNull();
    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }
}
