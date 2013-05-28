/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Resource;

import java.util.Map;

/**
 * @since 3.6
 */
public class ResourceCache implements BatchComponent {
  // resource by component key
  private final Map<String, Resource> resources = Maps.newHashMap();

  public Resource get(String componentKey) {
    return resources.get(componentKey);
  }

  public ResourceCache add(Resource resource) {
    String componentKey = resource.getEffectiveKey();
    Preconditions.checkState(!Strings.isNullOrEmpty(componentKey), "Missing resource effective key");
    Preconditions.checkState(!resources.containsKey(componentKey), "Resource is already registered: " + componentKey);
    resources.put(componentKey, resource);
    return this;
  }
}
