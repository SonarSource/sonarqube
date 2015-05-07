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

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.ServerSide;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @since 2.14
 */
@Beta
@ServerSide
public class ResourceTypes {

  public static final Predicate<ResourceType> AVAILABLE_FOR_FILTERS = new Predicate<ResourceType>() {
    @Override
    public boolean apply(@Nullable ResourceType input) {
      return input != null && input.getBooleanProperty("supportsMeasureFilters");
    }
  };

  private final Map<String, ResourceTypeTree> treeByQualifier;
  private final Map<String, ResourceType> typeByQualifier;
  private final Collection<ResourceType> rootTypes;

  public ResourceTypes() {
    this(new ResourceTypeTree[0]);
  }

  public ResourceTypes(ResourceTypeTree[] trees) {
    Preconditions.checkNotNull(trees);

    Map<String, ResourceTypeTree> treeMap = Maps.newHashMap();
    Map<String, ResourceType> typeMap = Maps.newLinkedHashMap();
    Collection<ResourceType> rootsSet = Sets.newHashSet();

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
    treeByQualifier = ImmutableMap.copyOf(treeMap);
    typeByQualifier = ImmutableMap.copyOf(typeMap);
    rootTypes = ImmutableList.copyOf(rootsSet);
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

  public Collection<ResourceType> getAll(Predicate<ResourceType> predicate) {
    return Collections2.filter(typeByQualifier.values(), predicate);
  }

  private static class PropertyKeyPredicate implements Predicate<ResourceType> {
    private final String propertyKey;

    public PropertyKeyPredicate(String propertyKey) {
      this.propertyKey = propertyKey;
    }

    @Override
    public boolean apply(@Nullable ResourceType input) {
      return input != null && input.hasProperty(propertyKey);
    }
  }

  public Collection<ResourceType> getAllWithPropertyKey(final String propertyKey) {
    return Collections2.filter(typeByQualifier.values(), new PropertyKeyPredicate(propertyKey));
  }

  private static class StringPropertyValuePredicate implements Predicate<ResourceType> {
    private final String propertyValue;
    private final String propertyKey;

    public StringPropertyValuePredicate(String propertyValue, String propertyKey) {
      this.propertyValue = propertyValue;
      this.propertyKey = propertyKey;
    }

    @Override
    public boolean apply(@Nullable ResourceType input) {
      return input != null && Objects.equal(propertyValue, input.getStringProperty(propertyKey));
    }
  }

  public Collection<ResourceType> getAllWithPropertyValue(final String propertyKey, final String propertyValue) {
    return Collections2.filter(typeByQualifier.values(), new StringPropertyValuePredicate(propertyValue, propertyKey));
  }

  private static class BooleanPropertyValuePredicate implements Predicate<ResourceType> {
    private final String propertyKey;
    private final boolean propertyValue;

    public BooleanPropertyValuePredicate(String propertyKey, boolean propertyValue) {
      this.propertyKey = propertyKey;
      this.propertyValue = propertyValue;
    }

    @Override
    public boolean apply(@Nullable ResourceType input) {
      return input != null && input.getBooleanProperty(propertyKey) == propertyValue;
    }
  }

  public Collection<ResourceType> getAllWithPropertyValue(final String propertyKey, final boolean propertyValue) {
    return Collections2.filter(typeByQualifier.values(), new BooleanPropertyValuePredicate(propertyKey, propertyValue));
  }

  public List<String> getChildrenQualifiers(String qualifier) {
    ResourceTypeTree tree = getTree(qualifier);
    if (tree != null) {
      return tree.getChildren(qualifier);
    }
    return Collections.emptyList();
  }

  public List<ResourceType> getChildren(String qualifier) {
    return Lists.transform(getChildrenQualifiers(qualifier), new Function<String, ResourceType>() {
      @Override
      public ResourceType apply(String s) {
        return typeByQualifier.get(s);
      }
    });
  }

  public List<String> getLeavesQualifiers(String qualifier) {
    ResourceTypeTree tree = getTree(qualifier);
    if (tree != null) {
      return tree.getLeaves();
    }
    return Collections.emptyList();
  }

  public ResourceTypeTree getTree(String qualifier) {
    return treeByQualifier.get(qualifier);
  }

  public ResourceType getRoot(String qualifier) {
    return getTree(qualifier).getRootType();
  }

}
