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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.ResourceTypesTest.qualifiers;

public class ResourceTypeTreeTest {
  private final ResourceTypeTree tree = ResourceTypeTree.builder()
    .addType(ResourceType.builder("TRK").build())
    .addType(ResourceType.builder("DIR").build())
    .addType(ResourceType.builder("FIL").build())
    .addType(ResourceType.builder("UTS").build())
    .addRelations("TRK", "DIR")
    .addRelations("DIR", "FIL")
    .addRelations("DIR", "UTS")
    .build();

  @Test
  public void getTypes() {
    assertThat(tree.getTypes()).hasSize(4);
    assertThat(qualifiers(tree.getTypes())).containsOnly("TRK", "DIR", "FIL", "UTS");
  }

  @Test
  public void getChildren() {
    assertThat(tree.getChildren("TRK")).containsOnly("DIR");

    assertThat(tree.getChildren("DIR")).containsOnly("FIL", "UTS");

    assertThat(tree.getChildren("FIL")).isEmpty();
  }

  @Test
  public void getRoot() {
    assertThat(tree.getRootType()).isEqualTo(ResourceType.builder("TRK").build());
  }

  @Test
  public void getLeaves() {
    assertThat(tree.getLeaves()).containsOnly("FIL", "UTS");
  }

  @Test(expected = IllegalArgumentException.class)
  public void forbidNullRelation() {
    ResourceTypeTree.builder()
      .addType(ResourceType.builder("TRK").build())
      .addType(ResourceType.builder("DIR").build())
      .addRelations("DIR" /* missing child */)
      .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void forbidDuplicatedType() {
    ResourceTypeTree.builder()
      .addType(ResourceType.builder("TRK").build())
      .addType(ResourceType.builder("TRK").build())
      .build();
  }
}
