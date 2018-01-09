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

import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * @since 2.14
 */
@Beta
@ServerSide
@ComputeEngineSide
public class ResourceTypes {

  private final Map<String, ResourceTypeTree> treeByQualifier;
  private final Map<String, ResourceType> typeByQualifier;
  private final Collection<ResourceType> rootTypes;

  public ResourceTypes(ResourceTypeTree[] trees) {
    requireNonNull(trees);

    Map<String, ResourceTypeTree> treeMap = new LinkedHashMap<>();
    Map<String, ResourceType> typeMap = new LinkedHashMap<>();
    Collection<ResourceType> rootsSet = new LinkedHashSet<>();

    for (ResourceTypeTree tree : trees) {
      rootsSet.add(tree.getRootType());
      for (ResourceType type : tree.getTypes()) {
        if (treeMap.containsKey(type.getQualifier())) {
          throw new IllegalStateException("Qualifier " + type.getQualifier() + " is defined in several trees");
        }
        treeMap.put(type.getQualifier(), tree);
        typeMap.put(type.getQualifier(), type);
      }
    }
    treeByQualifier = unmodifiableMap(new LinkedHashMap<>(treeMap));
    typeByQualifier = unmodifiableMap(new LinkedHashMap<>(typeMap));
    rootTypes = unmodifiableList(new ArrayList<>(rootsSet));
  }

  public ResourceType get(String qualifier) {
    ResourceType type = typeByQualifier.get(qualifier);
    return type != null ? type : ResourceType.builder(qualifier).build();
  }

  public Collection<ResourceType> getAll() {
    return typeByQualifier.values();
  }

  public Collection<ResourceType> getRoots() {
    return rootTypes;
  }

  public List<String> getLeavesQualifiers(String qualifier) {
    ResourceTypeTree tree = getTree(qualifier);
    if (tree != null) {
      return tree.getLeaves();
    }
    return Collections.emptyList();
  }

  private ResourceTypeTree getTree(String qualifier) {
    return treeByQualifier.get(qualifier);
  }
}
