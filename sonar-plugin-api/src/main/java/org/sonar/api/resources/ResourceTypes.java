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
package org.sonar.api.resources;

import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * @since 2.14
 */
@Beta
@ServerSide
@ComputeEngineSide
public class ResourceTypes {

  public static final Predicate<ResourceType> AVAILABLE_FOR_FILTERS = input -> input != null && input.getBooleanProperty("supportsMeasureFilters");

  private final Map<String, ResourceTypeTree> treeByQualifier;
  private final Map<String, ResourceType> typeByQualifier;
  private final Collection<ResourceType> orderedTypes;
  private final Collection<ResourceType> rootTypes;

  public ResourceTypes() {
    this(new ResourceTypeTree[0]);
  }

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
    orderedTypes = unmodifiableSet(orderedTypes(typeMap));
  }

  private static Set<ResourceType> orderedTypes(Map<String, ResourceType> typeByQualifier) {
    Map<String, ResourceType> mutableTypesByQualifier = new LinkedHashMap<>(typeByQualifier);
    ResourceType view = mutableTypesByQualifier.remove(Qualifiers.VIEW);
    ResourceType subView = mutableTypesByQualifier.remove(Qualifiers.SUBVIEW);
    ResourceType application = mutableTypesByQualifier.remove(Qualifiers.APP);

    return Stream.concat(Stream.of(view, subView, application), mutableTypesByQualifier.values().stream())
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public ResourceType get(String qualifier) {
    ResourceType type = typeByQualifier.get(qualifier);
    return type != null ? type : ResourceType.builder(qualifier).build();
  }

  public Collection<ResourceType> getAll() {
    return typeByQualifier.values();
  }

  public Collection<ResourceType> getAllOrdered() {
    return orderedTypes;
  }

  public Collection<ResourceType> getRoots() {
    return rootTypes;
  }

  public Collection<ResourceType> getAll(Predicate<ResourceType> predicate) {
    return typeByQualifier.values().stream()
      .filter(predicate)
      .collect(Collectors.toList());
  }

  public Collection<ResourceType> getAllWithPropertyKey(String propertyKey) {
    return typeByQualifier.values()
      .stream()
      .filter(Objects::nonNull)
      .filter(input -> input.hasProperty(propertyKey))
      .collect(Collectors.toList());
  }

  public Collection<ResourceType> getAllWithPropertyValue(String propertyKey, String propertyValue) {
    return typeByQualifier.values()
      .stream()
      .filter(Objects::nonNull)
      .filter(input -> Objects.equals(propertyValue, input.getStringProperty(propertyKey)))
      .collect(Collectors.toList());
  }

  public Collection<ResourceType> getAllWithPropertyValue(String propertyKey, boolean propertyValue) {
    return typeByQualifier.values()
      .stream()
      .filter(Objects::nonNull)
      .filter(input -> input.getBooleanProperty(propertyKey) == propertyValue)
      .collect(Collectors.toList());
  }

  public List<String> getChildrenQualifiers(String qualifier) {
    ResourceTypeTree tree = getTree(qualifier);
    if (tree != null) {
      return tree.getChildren(qualifier);
    }
    return Collections.emptyList();
  }

  public List<ResourceType> getChildren(String qualifier) {
    return getChildrenQualifiers(qualifier)
      .stream()
      .map(typeByQualifier::get)
      .collect(Collectors.toList());
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
