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
package org.sonar.api.resources;

import com.google.common.collect.Collections2;
import java.util.Collection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTypesTest {

  private ResourceTypeTree viewsTree = ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.VIEW).setProperty("supportsMeasureFilters", "true").build())
      .addType(ResourceType.builder(Qualifiers.SUBVIEW).build())
      .addRelations(Qualifiers.VIEW, Qualifiers.SUBVIEW)
      .addRelations(Qualifiers.SUBVIEW, Qualifiers.PROJECT)
      .build();

  private ResourceTypeTree applicationTree = ResourceTypeTree.builder()
    .addType(ResourceType.builder(Qualifiers.APP).setProperty("supportsMeasureFilters", "true").build())
    .addRelations(Qualifiers.APP, Qualifiers.PROJECT)
    .build();

  private ResourceTypeTree defaultTree = ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.PROJECT).setProperty("supportsMeasureFilters", "true").build())
      .addType(ResourceType.builder(Qualifiers.DIRECTORY).build())
      .addType(ResourceType.builder(Qualifiers.FILE).build())
      .addRelations(Qualifiers.PROJECT, Qualifiers.DIRECTORY)
      .addRelations(Qualifiers.DIRECTORY, Qualifiers.FILE)
      .build();

  private ResourceTypes types = new ResourceTypes(new ResourceTypeTree[] {defaultTree, viewsTree, applicationTree});

  @Test
  public void get() {
    assertThat(types.get(Qualifiers.PROJECT).getQualifier()).isEqualTo(Qualifiers.PROJECT);

    // does not return null
    assertThat(types.get("xxx").getQualifier()).isEqualTo("xxx");
  }

  @Test
  public void get_all() {
    assertThat(qualifiers(types.getAll())).containsExactly(Qualifiers.PROJECT, Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.VIEW, Qualifiers.SUBVIEW, Qualifiers.APP);
  }

  @Test
  public void get_roots() {
    assertThat(qualifiers(types.getRoots())).containsOnly(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);
  }

  @Test
  public void get_leaves_qualifiers() {
    assertThat(types.getLeavesQualifiers(Qualifiers.PROJECT)).containsExactly(Qualifiers.FILE);
    assertThat(types.getLeavesQualifiers(Qualifiers.DIRECTORY)).containsExactly(Qualifiers.FILE);
    assertThat(types.getLeavesQualifiers(Qualifiers.VIEW)).containsExactly(Qualifiers.PROJECT);
    assertThat(types.getLeavesQualifiers(Qualifiers.APP)).containsExactly(Qualifiers.PROJECT);
    assertThat(types.getLeavesQualifiers("xxx")).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_on_duplicated_qualifier() {
    ResourceTypeTree tree1 = ResourceTypeTree.builder()
        .addType(ResourceType.builder("foo").build())
        .build();
    ResourceTypeTree tree2 = ResourceTypeTree.builder()
        .addType(ResourceType.builder("foo").build())
        .build();

    new ResourceTypes(new ResourceTypeTree[] {tree1, tree2});
  }

  static Collection<String> qualifiers(Collection<ResourceType> types) {
    return Collections2.transform(types, ResourceType::getQualifier);
  }
}
