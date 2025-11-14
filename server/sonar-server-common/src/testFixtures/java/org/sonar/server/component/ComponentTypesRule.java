/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class ComponentTypesRule extends ComponentTypes {
  private Set<ComponentType> allComponentTypes = emptySet();
  private Set<ComponentType> rootComponentTypes = emptySet();
  private List<String> leavesQualifiers = emptyList();

  public ComponentTypesRule() {
    super(new ComponentTypeTree[0]);
  }


  @Override
  public Collection<ComponentType> getAll() {
    return allComponentTypes;
  }

  @Override
  public Collection<ComponentType> getRoots() {
    return rootComponentTypes;
  }

  public ComponentTypesRule setRootQualifiers(Collection<ComponentType> qualifiers) {
    rootComponentTypes = Set.copyOf(qualifiers);
    return this;
  }

  public ComponentTypesRule setRootQualifiers(String... qualifiers) {
    Set<ComponentType> componentTypes = new LinkedHashSet<>();
    for (String qualifier : qualifiers) {
      componentTypes.add(ComponentType.builder(qualifier).setProperty("deletable", true).build());
    }
    rootComponentTypes = Set.copyOf(componentTypes);

    return this;
  }

  public ComponentTypesRule setLeavesQualifiers(String... qualifiers) {
    leavesQualifiers = List.copyOf(Arrays.asList(qualifiers));
    return this;
  }

  public ComponentTypesRule setAllQualifiers(String... qualifiers) {
    Set<ComponentType> componentTypes = new HashSet<>();
    for (String qualifier : qualifiers) {
      componentTypes.add(ComponentType.builder(qualifier).setProperty("deletable", true).build());
    }
    allComponentTypes = Set.copyOf(componentTypes);

    return this;
  }

  public ComponentTypesRule setAllQualifiers(Collection<ComponentType> qualifiers) {
    allComponentTypes = Set.copyOf(qualifiers);
    return this;
  }

  @Override
  public ComponentType get(String qualifier) {
    return allComponentTypes.stream()
      .filter(resourceType -> qualifier.equals(resourceType.getQualifier()))
      .findAny().orElse(null);
  }

  @Override
  public boolean isQualifierPresent(String qualifier) {
    return rootComponentTypes.stream()
      .anyMatch(resourceType -> qualifier.equals(resourceType.getQualifier()));
  }

  @Override
  public List<String> getLeavesQualifiers(String qualifier) {
    return this.leavesQualifiers;
  }
}
