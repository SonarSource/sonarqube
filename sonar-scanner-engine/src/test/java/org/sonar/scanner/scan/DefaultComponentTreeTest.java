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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultComponentTreeTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultComponentTree tree;

  @Before
  public void setUp() {
    tree = new DefaultComponentTree();
  }

  @Test
  public void test() throws IOException {
    DefaultInputComponent root = new DefaultInputModule(ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputComponent mod1 = new DefaultInputModule(ProjectDefinition.create().setKey("mod1").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputComponent mod2 = new DefaultInputModule(ProjectDefinition.create().setKey("mod2").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputComponent mod3 = new DefaultInputModule(ProjectDefinition.create().setKey("mod3").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultInputComponent mod4 = new DefaultInputModule(ProjectDefinition.create().setKey("mod4").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));

    tree.index(mod1, root);
    tree.index(mod2, mod1);
    tree.index(mod3, root);
    tree.index(mod4, root);

    assertThat(tree.getChildren(root)).containsOnly(mod1, mod3, mod4);
    assertThat(tree.getChildren(mod4)).isEmpty();
    assertThat(tree.getChildren(mod1)).containsOnly(mod2);

    assertThat(tree.getParent(mod4)).isEqualTo(root);
    assertThat(tree.getParent(mod2)).isEqualTo(mod1);
    assertThat(tree.getParent(mod1)).isEqualTo(root);
    assertThat(tree.getParent(root)).isNull();
  }
}
