/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * @since 2.14
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class ResourceTypeTree {

  private final List<ResourceType> types;
  private final ListMultimap<String, String> relations;
  private final ResourceType root;

  private ResourceTypeTree(Builder builder) {
    this.types = unmodifiableList(new ArrayList<>(builder.types));
    this.relations = ImmutableListMultimap.copyOf(builder.relations);
    this.root = builder.root;
  }

  public List<ResourceType> getTypes() {
    return types;
  }

  public List<String> getChildren(String qualifier) {
    return relations.get(qualifier);
  }

  public ResourceType getRootType() {
    return root;
  }

  public List<String> getLeaves() {
    return unmodifiableList(relations.values()
      .stream()
      .filter(qualifier -> relations.get(qualifier).isEmpty())
      .collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    return root.getQualifier();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private List<ResourceType> types = new ArrayList<>();
    private ListMultimap<String, String> relations = ArrayListMultimap.create();
    private ResourceType root;

    private Builder() {
    }

    public Builder addType(ResourceType type) {
      requireNonNull(type);
      checkArgument(!types.contains(type), String.format("%s is already registered", type.getQualifier()));
      types.add(type);
      return this;
    }

    public Builder addRelations(String parentQualifier, String... childrenQualifiers) {
      requireNonNull(parentQualifier);
      requireNonNull(childrenQualifiers);
      checkArgument(childrenQualifiers.length > 0, "childrenQualifiers can't be empty");
      relations.putAll(parentQualifier, Arrays.asList(childrenQualifiers));
      return this;
    }

    public ResourceTypeTree build() {
      Collection<String> children = relations.values();
      for (ResourceType type : types) {
        if (!children.contains(type.getQualifier())) {
          root = type;
          break;
        }
      }
      return new ResourceTypeTree(this);
    }
  }

}
