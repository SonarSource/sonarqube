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
package org.sonar.core.component;

import com.google.common.base.Strings;
import org.sonar.api.component.Component;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;

import javax.annotation.Nullable;

public class ResourceComponent implements Component {
  private String key;
  private String path;
  private String name;
  private String longName;
  private String qualifier;
  private String scope;
  private Long snapshotId;
  private Long resourceId;

  public ResourceComponent(Resource resource, @Nullable Snapshot snapshot) {
    this.key = resource.getEffectiveKey();
    this.path = resource.getPath();
    if (Strings.isNullOrEmpty(key)) {
      throw new IllegalArgumentException("Missing component key");
    }
    this.name = resource.getName();
    this.longName = resource.getLongName();
    this.qualifier = resource.getQualifier();
    this.scope = resource.getScope();
    if (snapshot != null && snapshot.getId() != null) {
      this.snapshotId = snapshot.getId().longValue();
      this.resourceId = snapshot.getResourceId().longValue();
    }
  }

  public ResourceComponent(Resource resource) {
    this(resource, null);
  }

  public String key() {
    return key;
  }

  @Override
  public String path() {
    return path;
  }

  public String name() {
    return name;
  }

  public String longName() {
    return longName;
  }

  public String qualifier() {
    return qualifier;
  }

  public String scope() {
    return scope;
  }

  public Long snapshotId() {
    return snapshotId;
  }

  public Long resourceId() {
    return resourceId;
  }

  @Override
  public String toString() {
    return key;
  }
}
