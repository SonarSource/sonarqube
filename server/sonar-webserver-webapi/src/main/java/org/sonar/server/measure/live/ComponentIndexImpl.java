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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.emptyList;

public class ComponentIndexImpl implements ComponentIndex {
  private final DbClient dbClient;
  private ComponentDto branchComponent;
  private List<ComponentDto> sortedComponentsToRoot;
  private Map<String, List<ComponentDto>> children;

  public ComponentIndexImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Loads all the components required for the calculation of the new values of the live measures based on what components were modified:
   * - All components between the touched components and the roots of their component trees
   * - All immediate children of those components
   */
  public void load(DbSession dbSession, List<ComponentDto> touchedComponents) {
    sortedComponentsToRoot = loadTreeOfComponents(dbSession, touchedComponents);
    branchComponent = findBranchComponent(sortedComponentsToRoot);
    children = new HashMap<>();
    List<ComponentDto> childComponents = loadChildren(dbSession, branchComponent.uuid(), sortedComponentsToRoot);
    for (ComponentDto c : childComponents) {
      List<String> uuidPathAsList = c.getUuidPathAsList();
      String parentUuid = uuidPathAsList.get(uuidPathAsList.size() - 1);
      children.computeIfAbsent(parentUuid, uuid -> new LinkedList<>()).add(c);
    }
  }

  private static ComponentDto findBranchComponent(Collection<ComponentDto> components) {
    return components.stream().filter(ComponentDto::isRootProject).findFirst()
      .orElseThrow(() -> new IllegalStateException("No project found in " + components));
  }

  private List<ComponentDto> loadChildren(DbSession dbSession, String branchUuid, Collection<ComponentDto> components) {
    return dbClient.componentDao().selectChildren(dbSession, branchUuid, components);
  }

  private List<ComponentDto> loadTreeOfComponents(DbSession dbSession, List<ComponentDto> touchedComponents) {
    Set<String> componentUuids = new HashSet<>();
    for (ComponentDto component : touchedComponents) {
      componentUuids.add(component.uuid());
      // ancestors, excluding self
      componentUuids.addAll(component.getUuidPathAsList());
    }
    return dbClient.componentDao().selectByUuids(dbSession, componentUuids).stream()
      .sorted(Comparator.comparing(ComponentDto::getUuidPath).reversed())
      .toList();
  }

  @Override
  public List<ComponentDto> getChildren(ComponentDto component) {
    return children.getOrDefault(component.uuid(), emptyList());
  }

  @Override
  public Set<String> getAllUuids() {
    Set<String> all = new HashSet<>();
    sortedComponentsToRoot.forEach(c -> all.add(c.uuid()));
    for (Collection<ComponentDto> l : children.values()) {
      for (ComponentDto c : l) {
        all.add(c.uuid());
      }
    }
    return all;
  }

  @Override
  public List<ComponentDto> getSortedTree() {
    return sortedComponentsToRoot;
  }

  @Override public ComponentDto getBranch() {
    return branchComponent;
  }
}
