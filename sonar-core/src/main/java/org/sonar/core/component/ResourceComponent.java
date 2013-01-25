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
package org.sonar.core.component;

import org.sonar.api.component.Component;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;

import javax.annotation.Nullable;

class ResourceComponent implements Component {
  private String key;
  private String name;
  private String qualifier;
  private Long snapshotId;

  ResourceComponent(Resource resource, @Nullable Snapshot snapshot) {
    this.key = resource.getEffectiveKey();
    this.name = resource.getName();
    this.qualifier = resource.getQualifier();
    if (snapshot!=null && snapshot.getId()!=null) {
      this.snapshotId = snapshot.getId().longValue();
    }
  }

  ResourceComponent(Resource resource) {
    this(resource, null);
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getQualifier() {
    return qualifier;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }
}