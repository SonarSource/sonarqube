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
package org.sonar.db.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class ResourceTypesRule extends ResourceTypes {
  private Set<ResourceType> allResourceTypes = emptySet();
  private Set<ResourceType> rootResourceTypes = emptySet();
  private List<String> childrenQualifiers = emptyList();
  private List<String> leavesQualifiers = emptyList();

  @Override
  public Collection<ResourceType> getAll() {
    return allResourceTypes;
  }

  @Override
  public Collection<ResourceType> getAllOrdered() {
    return allResourceTypes;
  }

  @Override
  public Collection<ResourceType> getRoots() {
    return rootResourceTypes;
  }

  public ResourceTypesRule setRootQualifiers(String... qualifiers) {
    Set<ResourceType> resourceTypes = new LinkedHashSet<>();
    for (String qualifier : qualifiers) {
      resourceTypes.add(ResourceType.builder(qualifier).build());
    }
    rootResourceTypes = ImmutableSet.copyOf(resourceTypes);

    return this;
  }

  public ResourceTypesRule setLeavesQualifiers(String... qualifiers) {
    leavesQualifiers = ImmutableList.copyOf(qualifiers);
    return this;
  }

  public ResourceTypesRule setChildrenQualifiers(String... qualifiers) {
    childrenQualifiers = ImmutableList.copyOf(qualifiers);
    return this;
  }

  public ResourceTypesRule setAllQualifiers(String... qualifiers) {
    Set<ResourceType> resourceTypes = new HashSet<>();
    for (String qualifier : qualifiers) {
      resourceTypes.add(ResourceType.builder(qualifier).build());
    }
    allResourceTypes = ImmutableSet.copyOf(resourceTypes);

    return this;
  }

  @Override
  public ResourceType get(String qualifier) {
    return allResourceTypes.stream()
      .filter(resourceType -> qualifier.equals(resourceType.getQualifier()))
      .findAny().orElse(null);
  }

  @Override
  public Collection<ResourceType> getAllWithPropertyKey(String propertyKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<ResourceType> getAllWithPropertyValue(String propertyKey, String propertyValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<ResourceType> getAllWithPropertyValue(String propertyKey, boolean propertyValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getChildrenQualifiers(String qualifier) {
    return this.childrenQualifiers;
  }

  @Override
  public List<ResourceType> getChildren(String qualifier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getLeavesQualifiers(String qualifier) {
    return this.leavesQualifiers;
  }

  @Override
  public ResourceTypeTree getTree(String qualifier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResourceType getRoot(String qualifier) {
    throw new UnsupportedOperationException();
  }
}
