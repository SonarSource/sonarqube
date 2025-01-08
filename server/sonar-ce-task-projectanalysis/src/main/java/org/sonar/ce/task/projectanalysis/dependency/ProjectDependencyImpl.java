/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.dependency;

import com.google.common.base.MoreObjects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_DESCRIPTION_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;

@Immutable
public class ProjectDependencyImpl implements ProjectDependency {
  private final String fullName;
  private final String name;
  private final String key;
  private final String uuid;

  @CheckForNull
  private final String description;
  @CheckForNull
  private final String version;
  @CheckForNull
  private final String packageManager;

  private ProjectDependencyImpl(Builder builder) {
    this.key = builder.key;
    this.fullName = builder.fullName;
    this.name = MoreObjects.firstNonNull(builder.name, builder.fullName).intern();
    this.description = builder.description;
    this.uuid = builder.uuid;
    this.version = builder.version;
    this.packageManager = builder.packageManager;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getFullName() {
    return this.fullName;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  @CheckForNull
  @Override
  public String getVersion() {
    return version;
  }

  @CheckForNull
  @Override
  public String getPackageManager() {
    return packageManager;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private static final String KEY_CANNOT_BE_NULL = "Key can't be null";
    private static final String UUID_CANNOT_BE_NULL = "uuid can't be null";
    private static final String NAME_CANNOT_BE_NULL = "name can't be null";

    private String uuid;
    private String key;
    private String fullName;
    private String name;
    private String description;
    private String version;
    private String packageManager;

    public Builder setUuid(String s) {
      this.uuid = requireNonNull(s, UUID_CANNOT_BE_NULL);
      return this;
    }

    public Builder setKey(String key) {
      this.key = requireNonNull(key, KEY_CANNOT_BE_NULL);
      return this;
    }

    public Builder setFullName(String fullName) {
      this.fullName = abbreviate(requireNonNull(fullName, NAME_CANNOT_BE_NULL), MAX_COMPONENT_NAME_LENGTH);
      return this;
    }

    public Builder setName(String name) {
      this.name = abbreviate(requireNonNull(name, NAME_CANNOT_BE_NULL), MAX_COMPONENT_NAME_LENGTH);
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = abbreviate(trimToNull(description), MAX_COMPONENT_DESCRIPTION_LENGTH);
      return this;
    }

    public Builder setVersion(@Nullable String version) {
      this.version = trimToNull(version);
      return this;
    }

    public Builder setPackageManager(@Nullable String packageManager) {
      this.packageManager = trimToNull(packageManager);
      return this;
    }

    public ProjectDependencyImpl build() {
      requireNonNull(uuid, UUID_CANNOT_BE_NULL);
      requireNonNull(key, KEY_CANNOT_BE_NULL);
      requireNonNull(name, NAME_CANNOT_BE_NULL);
      return new ProjectDependencyImpl(this);
    }

  }

  @Override
  public String toString() {
    return "ComponentImpl{" +
      "fullName='" + fullName + '\'' +
      ", key='" + key + '\'' +
      ", uuid='" + uuid + '\'' +
      ", description='" + description + '\'' +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDependencyImpl component = (ProjectDependencyImpl) o;
    return uuid.equals(component.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
