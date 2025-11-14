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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@ServerSide
@ComputeEngineSide
public class ComponentTypeTree {

  private final List<ComponentType> types;
  private final Map<String, List<String>> relations;
  private final ComponentType root;

  private ComponentTypeTree(Builder builder) {
    this.types = unmodifiableList(new ArrayList<>(builder.types));
    this.relations = Collections.unmodifiableMap(builder.relations);
    this.root = builder.root;
  }

  public List<ComponentType> getTypes() {
    return types;
  }

  public List<String> getChildren(String qualifier) {
    return relations.getOrDefault(qualifier, Collections.emptyList());
  }

  public ComponentType getRootType() {
    return root;
  }

  public List<String> getLeaves() {
    return relations.values()
      .stream()
      .flatMap(Collection::stream)
      .filter(qualifier -> !relations.containsKey(qualifier))
      .toList();
  }

  @Override
  public String toString() {
    return root.getQualifier();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final List<ComponentType> types = new ArrayList<>();
    private final Map<String, List<String>> relations = new HashMap<>();
    private final List<String> children = new ArrayList<>();
    private ComponentType root;

    private Builder() {
    }

    public Builder addType(ComponentType type) {
      requireNonNull(type);
      checkArgument(!types.contains(type), String.format("%s is already registered", type.getQualifier()));
      types.add(type);
      return this;
    }

    public Builder addRelations(String parentQualifier, String... childrenQualifiers) {
      requireNonNull(parentQualifier);
      requireNonNull(childrenQualifiers);
      checkArgument(childrenQualifiers.length > 0, "childrenQualifiers can't be empty");
      relations.computeIfAbsent(parentQualifier, x -> new ArrayList<>()).addAll(Arrays.asList(childrenQualifiers));
      children.addAll(Arrays.asList(childrenQualifiers));
      return this;
    }

    public ComponentTypeTree build() {
      for (ComponentType type : types) {
        if (!children.contains(type.getQualifier())) {
          root = type;
          break;
        }
      }
      return new ComponentTypeTree(this);
    }
  }

}
