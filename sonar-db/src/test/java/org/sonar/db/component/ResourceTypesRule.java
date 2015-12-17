/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;

public class ResourceTypesRule extends ResourceTypes {
  private final Set<ResourceType> allResourceTypes = new HashSet<>();
  private final Set<ResourceType> rootResourceTypes = new HashSet<>();
  private final List<String> childrenQualifiers = new ArrayList<>();
  private final List<String> leavesQualifiers = new ArrayList<>();

  @Override
  public Collection<ResourceType> getAll() {
    return allResourceTypes;
  }

  @Override
  public Collection<ResourceType> getRoots() {
    return rootResourceTypes;
  }

  public ResourceTypesRule setRootQualifiers(String... qualifiers) {
    rootResourceTypes.clear();
    for (String qualifier : qualifiers) {
      rootResourceTypes.add(ResourceType.builder(qualifier).build());
    }

    return this;
  }

  public ResourceTypesRule setLeavesQualifiers(String... qualifiers) {
    leavesQualifiers.clear();
    leavesQualifiers.addAll(Arrays.asList(qualifiers));
    return this;
  }

  public ResourceTypesRule setChildrenQualifiers(String... qualifiers) {
    childrenQualifiers.clear();
    childrenQualifiers.addAll(Arrays.asList(qualifiers));

    return this;
  }

  public ResourceTypesRule setAllQualifiers(String... qualifiers) {
    allResourceTypes.clear();
    for (String qualifier : qualifiers) {
      allResourceTypes.add(ResourceType.builder(qualifier).build());
    }

    return this;
  }

  @Override
  public ResourceType get(String qualifier) {
    throw new UnsupportedOperationException();
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
