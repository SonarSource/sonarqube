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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

public class DefaultInputModuleHierarchyTest {
  private DefaultInputModuleHierarchy moduleHierarchy;

  @Before
  public void setUp() {
    moduleHierarchy = new DefaultInputModuleHierarchy();
  }

  @Test
  public void test() {
    DefaultInputModule root = new DefaultInputModule("root");
    DefaultInputModule mod1 = new DefaultInputModule("mod1");
    DefaultInputModule mod2 = new DefaultInputModule("mod2");
    DefaultInputModule mod3 = new DefaultInputModule("mod3");
    DefaultInputModule mod4 = new DefaultInputModule("mod4");

    moduleHierarchy.setRoot(root);
    moduleHierarchy.index(mod1, root);
    moduleHierarchy.index(mod2, mod1);
    moduleHierarchy.index(mod3, root);
    moduleHierarchy.index(mod4, root);

    assertThat(moduleHierarchy.children(root)).containsOnly(mod1, mod3, mod4);
    assertThat(moduleHierarchy.children(mod4)).isEmpty();
    assertThat(moduleHierarchy.children(mod1)).containsOnly(mod2);

    assertThat(moduleHierarchy.parent(mod4)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(mod2)).isEqualTo(mod1);
    assertThat(moduleHierarchy.parent(mod1)).isEqualTo(root);
    assertThat(moduleHierarchy.parent(root)).isNull();

    assertThat(moduleHierarchy.root()).isEqualTo(root);
  }
}
