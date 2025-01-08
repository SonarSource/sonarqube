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
package org.sonar.server.component;

import com.google.common.collect.Collections2;
import java.util.Collection;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTypesTest {

  private ComponentTypeTree viewsTree = ComponentTypeTree.builder()
      .addType(ComponentType.builder(ComponentQualifiers.VIEW).setProperty("supportsMeasureFilters", "true").build())
      .addType(ComponentType.builder(ComponentQualifiers.SUBVIEW).build())
      .addRelations(ComponentQualifiers.VIEW, ComponentQualifiers.SUBVIEW)
      .addRelations(ComponentQualifiers.SUBVIEW, ComponentQualifiers.PROJECT)
      .build();

  private ComponentTypeTree applicationTree = ComponentTypeTree.builder()
    .addType(ComponentType.builder(ComponentQualifiers.APP).setProperty("supportsMeasureFilters", "true").build())
    .addRelations(ComponentQualifiers.APP, ComponentQualifiers.PROJECT)
    .build();

  private ComponentTypeTree defaultTree = ComponentTypeTree.builder()
      .addType(ComponentType.builder(ComponentQualifiers.PROJECT).setProperty("supportsMeasureFilters", "true").build())
      .addType(ComponentType.builder(ComponentQualifiers.DIRECTORY).build())
      .addType(ComponentType.builder(ComponentQualifiers.FILE).build())
      .addRelations(ComponentQualifiers.PROJECT, ComponentQualifiers.DIRECTORY)
      .addRelations(ComponentQualifiers.DIRECTORY, ComponentQualifiers.FILE)
      .build();

  private ComponentTypes types = new ComponentTypes(new ComponentTypeTree[] {defaultTree, viewsTree, applicationTree});

  @Test
  public void get() {
    assertThat(types.get(ComponentQualifiers.PROJECT).getQualifier()).isEqualTo(ComponentQualifiers.PROJECT);

    // does not return null
    assertThat(types.get("xxx").getQualifier()).isEqualTo("xxx");
  }

  @Test
  public void get_all() {
    assertThat(qualifiers(types.getAll())).containsExactly(ComponentQualifiers.PROJECT, ComponentQualifiers.DIRECTORY, ComponentQualifiers.FILE, ComponentQualifiers.VIEW, ComponentQualifiers.SUBVIEW, ComponentQualifiers.APP);
  }

  @Test
  public void get_roots() {
    assertThat(qualifiers(types.getRoots())).containsOnly(ComponentQualifiers.PROJECT, ComponentQualifiers.VIEW, ComponentQualifiers.APP);
  }

  @Test
  public void get_leaves_qualifiers() {
    assertThat(types.getLeavesQualifiers(ComponentQualifiers.PROJECT)).containsExactly(ComponentQualifiers.FILE);
    assertThat(types.getLeavesQualifiers(ComponentQualifiers.DIRECTORY)).containsExactly(ComponentQualifiers.FILE);
    assertThat(types.getLeavesQualifiers(ComponentQualifiers.VIEW)).containsExactly(ComponentQualifiers.PROJECT);
    assertThat(types.getLeavesQualifiers(ComponentQualifiers.APP)).containsExactly(ComponentQualifiers.PROJECT);
    assertThat(types.getLeavesQualifiers("xxx")).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_on_duplicated_qualifier() {
    ComponentTypeTree tree1 = ComponentTypeTree.builder()
        .addType(ComponentType.builder("foo").build())
        .build();
    ComponentTypeTree tree2 = ComponentTypeTree.builder()
        .addType(ComponentType.builder("foo").build())
        .build();

    new ComponentTypes(new ComponentTypeTree[] {tree1, tree2});
  }

  @Test
  public void isQualifierPresent() {
    assertThat(types.isQualifierPresent(ComponentQualifiers.APP)).isTrue();
    assertThat(types.isQualifierPresent(ComponentQualifiers.VIEW)).isTrue();
    assertThat(types.isQualifierPresent("XXXX")).isFalse();
  }

  static Collection<String> qualifiers(Collection<ComponentType> types) {
    return Collections2.transform(types, ComponentType::getQualifier);
  }
}
