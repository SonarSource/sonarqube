/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.Test;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceTypesTest {

  private ResourceTypeTree viewsTree = ResourceTypeTree.builder()
    .addType(ResourceType.builder(Qualifiers.VIEW).setProperty("supports_measure_filters", "true").build())
    .addType(ResourceType.builder(Qualifiers.SUBVIEW).build())
    .addRelations(Qualifiers.VIEW, Qualifiers.SUBVIEW)
    .addRelations(Qualifiers.SUBVIEW, Qualifiers.PROJECT)
    .build();

  private ResourceTypeTree defaultTree = ResourceTypeTree.builder()
    .addType(ResourceType.builder(Qualifiers.PROJECT).setProperty("supports_measure_filters", "true").build())
    .addType(ResourceType.builder(Qualifiers.DIRECTORY).build())
    .addType(ResourceType.builder(Qualifiers.FILE).build())
    .addRelations(Qualifiers.PROJECT, Qualifiers.DIRECTORY)
    .addRelations(Qualifiers.DIRECTORY, Qualifiers.FILE)
    .build();

  private ResourceTypes types = new ResourceTypes(new ResourceTypeTree[]{viewsTree, defaultTree});

  @Test
  public void get() {
    assertThat(types.get(Qualifiers.PROJECT).getQualifier()).isEqualTo(Qualifiers.PROJECT);

    // does not return null
    assertThat(types.get("xxx").getQualifier()).isEqualTo("xxx");
  }

  @Test
  public void getAll() {
    assertThat(qualifiers(types.getAll())).containsOnly(Qualifiers.PROJECT, Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  @Test
  public void getAll_predicate() {
    Collection<ResourceType> forFilters = types.getAll(ResourceTypes.AVAILABLE_FOR_FILTERS);
    assertThat(qualifiers(forFilters)).containsOnly(Qualifiers.PROJECT, Qualifiers.VIEW).doesNotHaveDuplicates();
  }

  @Test
  public void getAllWithPropertyKey() {
    assertThat(qualifiers(types.getAllWithPropertyKey("supports_measure_filters"))).containsOnly(Qualifiers.VIEW, Qualifiers.PROJECT);
  }

  @Test
  public void getAllWithPropertyValue() {
    assertThat(qualifiers(types.getAllWithPropertyValue("supports_measure_filters", "true"))).containsOnly(Qualifiers.VIEW, Qualifiers.PROJECT);
    assertThat(qualifiers(types.getAllWithPropertyValue("supports_measure_filters", true))).containsOnly(Qualifiers.VIEW, Qualifiers.PROJECT);
    assertThat(qualifiers(types.getAllWithPropertyValue("supports_measure_filters", false))).containsOnly(Qualifiers.SUBVIEW, Qualifiers.DIRECTORY, Qualifiers.FILE);
  }

  @Test
  public void getChildrenQualifiers() {
    assertThat(types.getChildrenQualifiers(Qualifiers.PROJECT)).containsExactly(Qualifiers.DIRECTORY);
    assertThat(types.getChildrenQualifiers(Qualifiers.SUBVIEW)).containsExactly(Qualifiers.PROJECT);
    assertThat(types.getChildrenQualifiers("xxx")).isEmpty();
    assertThat(types.getChildrenQualifiers(Qualifiers.FILE)).isEmpty();
  }

  @Test
  public void getChildren() {
    assertThat(qualifiers(types.getChildren(Qualifiers.PROJECT))).contains(Qualifiers.DIRECTORY);
    assertThat(qualifiers(types.getChildren(Qualifiers.SUBVIEW))).contains(Qualifiers.PROJECT);
  }

  @Test
  public void getLeavesQualifiers() {
    assertThat(types.getLeavesQualifiers(Qualifiers.PROJECT)).containsExactly(Qualifiers.FILE);

    assertThat(types.getLeavesQualifiers(Qualifiers.DIRECTORY)).containsExactly(Qualifiers.FILE);

    assertThat(types.getLeavesQualifiers(Qualifiers.VIEW)).containsExactly(Qualifiers.PROJECT);

    assertThat(types.getLeavesQualifiers("xxx")).isEmpty();
  }

  @Test
  public void getTree() {
    assertThat(qualifiers(types.getTree(Qualifiers.VIEW).getTypes())).containsOnly(Qualifiers.VIEW, Qualifiers.SUBVIEW).doesNotHaveDuplicates();
    assertThat(types.getTree("xxx")).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void failOnDuplicatedQualifier() {
    ResourceTypeTree tree1 = ResourceTypeTree.builder()
      .addType(ResourceType.builder("foo").build())
      .build();
    ResourceTypeTree tree2 = ResourceTypeTree.builder()
      .addType(ResourceType.builder("foo").build())
      .build();

    new ResourceTypes(new ResourceTypeTree[]{tree1, tree2});
  }

  static Collection<String> qualifiers(Collection<ResourceType> types) {
    return Collections2.transform(types, new Function<ResourceType, String>() {
      public String apply(ResourceType type) {
        return type.getQualifier();
      }
    });
  }
}
