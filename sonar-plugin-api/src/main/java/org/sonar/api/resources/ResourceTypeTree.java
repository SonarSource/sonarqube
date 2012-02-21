/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.List;

/**
 * @since 2.14
 */
@Beta
@Immutable
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public final class ResourceTypeTree implements BatchExtension, ServerExtension {

  private List<ResourceType> types;
  private ListMultimap<String, String> relations;

  private ResourceTypeTree(Builder builder) {
    this.types = ImmutableList.copyOf(builder.types);
    this.relations = ImmutableListMultimap.copyOf(builder.relations);
  }

  public List<ResourceType> getTypes() {
    return types;
  }

  public List<String> getChildren(String qualifier) {
    return relations.get(qualifier);
  }

  public List<String> getLeaves() {
    return ImmutableList.copyOf(Collections2.filter(relations.values(), new Predicate<String>() {
      public boolean apply(String qualifier) {
        return relations.get(qualifier).isEmpty();
      }
    }));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<ResourceType> types = Lists.newArrayList();
    private ListMultimap<String, String> relations = ArrayListMultimap.create();

    private Builder() {
    }

    public Builder addType(ResourceType type) {
      Preconditions.checkNotNull(type);
      Preconditions.checkArgument(!types.contains(type), String.format("%s is already registered", type.getQualifier()));
      types.add(type);
      return this;
    }

    public Builder addRelations(String parentQualifier, String... childQualifiers) {
      Preconditions.checkNotNull(parentQualifier);
      Preconditions.checkArgument(ArrayUtils.isNotEmpty(childQualifiers), "childQualifiers can't be empty");
      relations.putAll(parentQualifier, Arrays.asList(childQualifiers));
      return this;
    }

    public ResourceTypeTree build() {
      return new ResourceTypeTree(this);
    }
  }

}
