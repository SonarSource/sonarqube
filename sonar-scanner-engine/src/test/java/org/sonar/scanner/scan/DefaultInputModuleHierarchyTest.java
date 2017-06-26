/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

public class DefaultInputModuleHierarchyTest {
  private DefaultInputModuleHierarchy moduleHierarchy;

  @Test
  public void test() {
    DefaultInputModule root = new DefaultInputModule("root");
    DefaultInputModule mod1 = new DefaultInputModule("mod1");
    DefaultInputModule mod2 = new DefaultInputModule("mod2");
    DefaultInputModule mod3 = new DefaultInputModule("mod3");
    DefaultInputModule mod4 = new DefaultInputModule("mod4");

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
  public void testOnlyRoot() {
    DefaultInputModule root = new DefaultInputModule("root");
    moduleHierarchy = new DefaultInputModuleHierarchy(root);

    assertThat(moduleHierarchy.children(root)).isEmpty();
    assertThat(moduleHierarchy.parent(root)).isNull();
    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }

  @Test
  public void testParentChild() {
    DefaultInputModule root = new DefaultInputModule("root");
    DefaultInputModule mod1 = new DefaultInputModule("mod1");
    moduleHierarchy = new DefaultInputModuleHierarchy(root, mod1);

    assertThat(moduleHierarchy.children(root)).containsOnly(mod1);
    assertThat(moduleHierarchy.children(mod1)).isEmpty();

    assertThat(moduleHierarchy.parent(mod1)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(root)).isNull();
    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }
}
