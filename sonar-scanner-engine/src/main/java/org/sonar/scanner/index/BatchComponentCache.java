/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.resources.Resource;

@ScannerSide
public class BatchComponentCache {
  // components by key
  private final Map<String, BatchComponent> components = Maps.newLinkedHashMap();

  private BatchComponent root;

  @CheckForNull
  public BatchComponent get(String componentKey) {
    return components.get(componentKey);
  }

  public BatchComponent get(Resource resource) {
    return components.get(resource.getEffectiveKey());
  }

  public BatchComponent get(InputComponent inputComponent) {
    return components.get(inputComponent.key());
  }

  public BatchComponent add(Resource resource, @Nullable Resource parentResource) {
    String componentKey = resource.getEffectiveKey();
    Preconditions.checkState(!Strings.isNullOrEmpty(componentKey), "Missing resource effective key");
    BatchComponent parent = parentResource != null ? get(parentResource.getEffectiveKey()) : null;
    BatchComponent batchComponent = new BatchComponent(components.size() + 1, resource, parent);
    components.put(componentKey, batchComponent);
    if (parent == null) {
      root = batchComponent;
    }
    return batchComponent;
  }

  public Collection<BatchComponent> all() {
    return components.values();
  }

  public BatchComponent getRoot() {
    return root;
  }
}
