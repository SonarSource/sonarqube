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
package org.sonar.api.resources;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.14
 */
@BatchSide
@ServerSide
public class ResourceTypeTree {

  private final List<ResourceType> types;
  private final ListMultimap<String, String> relations;
  private final ResourceType root;

  private ResourceTypeTree(Builder builder) {
    this.types = ImmutableList.copyOf(builder.types);
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
    return ImmutableList.copyOf(Collections2.filter(relations.values(), new Predicate<String>() {
      @Override
      public boolean apply(String qualifier) {
        return relations.get(qualifier).isEmpty();
      }
    }));
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
      Preconditions.checkNotNull(type);
      Preconditions.checkArgument(!types.contains(type), String.format("%s is already registered", type.getQualifier()));
      types.add(type);
      return this;
    }

    public Builder addRelations(String parentQualifier, String... childrenQualifiers) {
      Preconditions.checkNotNull(parentQualifier);
      Preconditions.checkNotNull(childrenQualifiers);
      Preconditions.checkArgument(childrenQualifiers.length > 0, "childrenQualifiers can't be empty");
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
