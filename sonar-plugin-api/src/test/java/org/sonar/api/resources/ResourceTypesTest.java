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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class ResourceTypesTest {

  private ResourceTypeTree viewsTree = ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.VIEW).setProperty("availableForFilters", "true").build())
      .addType(ResourceType.builder(Qualifiers.SUBVIEW).build())
      .addRelations(Qualifiers.VIEW, Qualifiers.SUBVIEW)
      .addRelations(Qualifiers.SUBVIEW, Qualifiers.PROJECT)
      .build();

  private ResourceTypeTree defaultTree = ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.PROJECT).setProperty("availableForFilters", "true").build())
      .addType(ResourceType.builder(Qualifiers.DIRECTORY).build())
      .addType(ResourceType.builder(Qualifiers.FILE).build())
      .addRelations(Qualifiers.PROJECT, Qualifiers.DIRECTORY)
      .addRelations(Qualifiers.DIRECTORY, Qualifiers.FILE)
      .build();

  private ResourceTypes types = new ResourceTypes(new ResourceTypeTree[] {viewsTree, defaultTree});

  @Test
  public void get() {
    assertThat(types.get(Qualifiers.PROJECT).getQualifier(), is(Qualifiers.PROJECT));

    // does not return null
    assertThat(types.get("xxx").getQualifier(), is("xxx"));
  }

  @Test
  public void getAll() {
    assertThat(types.getAll().size(), is(5));
    assertThat(qualifiers(types.getAll()), hasItems(
        Qualifiers.PROJECT, Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.VIEW, Qualifiers.SUBVIEW));
  }

  @Test
  public void getAll_predicate() {
    Collection<ResourceType> forFilters = types.getAll(ResourceTypes.AVAILABLE_FOR_FILTERS);
    assertThat(forFilters.size(), is(2));
    assertThat(qualifiers(forFilters), hasItems(Qualifiers.PROJECT, Qualifiers.VIEW));
  }

  @Test
  public void getChildrenQualifiers() {
    assertThat(types.getChildrenQualifiers(Qualifiers.PROJECT).size(), is(1));
    assertThat(types.getChildrenQualifiers(Qualifiers.PROJECT), hasItem(Qualifiers.DIRECTORY));
    assertThat(types.getChildrenQualifiers(Qualifiers.SUBVIEW), hasItem(Qualifiers.PROJECT));
    assertThat(types.getChildrenQualifiers("xxx").isEmpty(), is(true));
    assertThat(types.getChildrenQualifiers(Qualifiers.FILE).isEmpty(), is(true));
  }

  @Test
  public void getChildren() {
    assertThat(qualifiers(types.getChildren(Qualifiers.PROJECT)), hasItem(Qualifiers.DIRECTORY));
    assertThat(qualifiers(types.getChildren(Qualifiers.SUBVIEW)), hasItem(Qualifiers.PROJECT));
  }

  @Test
  public void getLeavesQualifiers() {
    assertThat(types.getLeavesQualifiers(Qualifiers.PROJECT).size(), is(1));
    assertThat(types.getLeavesQualifiers(Qualifiers.PROJECT), hasItem(Qualifiers.FILE));

    assertThat(types.getLeavesQualifiers(Qualifiers.DIRECTORY).size(), is(1));
    assertThat(types.getLeavesQualifiers(Qualifiers.DIRECTORY), hasItem(Qualifiers.FILE));

    assertThat(types.getLeavesQualifiers(Qualifiers.VIEW).size(), is(1));
    assertThat(types.getLeavesQualifiers(Qualifiers.VIEW), hasItem(Qualifiers.PROJECT));

    assertThat(types.getLeavesQualifiers("xxx").size(), is(0));
  }

  @Test
  public void getTree() {
    assertThat(qualifiers(types.getTree(Qualifiers.VIEW).getTypes()), hasItems(Qualifiers.VIEW, Qualifiers.SUBVIEW));
    assertThat(types.getTree("xxx"), nullValue());
  }

  @Test(expected = IllegalStateException.class)
  public void failOnDuplicatedQualifier() {
    ResourceTypeTree tree1 = ResourceTypeTree.builder()
        .addType(ResourceType.builder("foo").build())
        .build();
    ResourceTypeTree tree2 = ResourceTypeTree.builder()
        .addType(ResourceType.builder("foo").build())
        .build();

    new ResourceTypes(new ResourceTypeTree[] {tree1, tree2});
  }

  static Collection<String> qualifiers(Collection<ResourceType> types) {
    return Collections2.transform(types, new Function<ResourceType, String>() {
      public String apply(ResourceType type) {
        return type.getQualifier();
      }
    });
  }
}
