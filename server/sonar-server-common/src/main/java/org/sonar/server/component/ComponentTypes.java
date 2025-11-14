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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@ServerSide
@ComputeEngineSide
public class ComponentTypes {

  private final Map<String, ComponentTypeTree> treeByQualifier;
  private final Map<String, ComponentType> typeByQualifier;
  private final Collection<ComponentType> rootTypes;

  public ComponentTypes(ComponentTypeTree[] trees) {
    requireNonNull(trees);

    Map<String, ComponentTypeTree> treeMap = new LinkedHashMap<>();
    Map<String, ComponentType> typeMap = new LinkedHashMap<>();
    Collection<ComponentType> rootsSet = new LinkedHashSet<>();

    for (ComponentTypeTree tree : trees) {
      rootsSet.add(tree.getRootType());
      for (ComponentType type : tree.getTypes()) {
        if (treeMap.containsKey(type.getQualifier())) {
          throw new IllegalStateException("Qualifier " + type.getQualifier() + " is defined in several trees");
        }
        treeMap.put(type.getQualifier(), tree);
        typeMap.put(type.getQualifier(), type);
      }
    }
    treeByQualifier = unmodifiableMap(new LinkedHashMap<>(treeMap));
    typeByQualifier = unmodifiableMap(new LinkedHashMap<>(typeMap));
    rootTypes = List.copyOf(rootsSet);
  }

  public ComponentType get(String qualifier) {
    ComponentType type = typeByQualifier.get(qualifier);
    return type != null ? type : ComponentType.builder(qualifier).build();
  }

  public Collection<ComponentType> getAll() {
    return typeByQualifier.values();
  }

  public Collection<ComponentType> getRoots() {
    return rootTypes;
  }

  public boolean isQualifierPresent(String qualifier) {
    return typeByQualifier.get(qualifier) != null;
  }

  public List<String> getLeavesQualifiers(String qualifier) {
    ComponentTypeTree tree = getTree(qualifier);
    if (tree != null) {
      return tree.getLeaves();
    }
    return Collections.emptyList();
  }

  private ComponentTypeTree getTree(String qualifier) {
    return treeByQualifier.get(qualifier);
  }
}
