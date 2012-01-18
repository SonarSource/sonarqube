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
package org.sonar.server.ui;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.ServerComponent;
import org.sonar.api.resources.ResourceDefinition;

import java.util.Collection;
import java.util.Map;

public class ResourceDefinitionRepository implements ServerComponent {

  private final Map<String, ResourceDefinition> descriptionsByQualifier;

  public ResourceDefinitionRepository(ResourceDefinition[] descriptions) {
    ImmutableMap.Builder<String, ResourceDefinition> map = ImmutableMap.builder();
    for (ResourceDefinition description : descriptions) {
      map.put(description.getQualifier(), description);
    }
    descriptionsByQualifier = map.build();
  }

  public ResourceDefinition get(String qualifier) {
    ResourceDefinition result = descriptionsByQualifier.get(qualifier);
    if (result != null) {
      return result;
    }
    // to avoid NPE on ruby side
    return ResourceDefinition.builder(qualifier).build();
  }

  public Collection<ResourceDefinition> getAll() {
    return descriptionsByQualifier.values();
  }

  public Collection<ResourceDefinition> getForFilter() {
    return ImmutableList.copyOf(Collections2.filter(descriptionsByQualifier.values(), IS_AVAILABLE_FOR_FILTER));
  }

  private static final Predicate<ResourceDefinition> IS_AVAILABLE_FOR_FILTER = new Predicate<ResourceDefinition>() {
    public boolean apply(ResourceDefinition input) {
      return input.isAvailableForFilters();
    }
  };

}
