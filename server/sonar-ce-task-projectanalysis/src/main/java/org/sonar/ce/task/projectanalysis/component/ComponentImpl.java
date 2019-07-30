/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_DESCRIPTION_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;

@Immutable
public class ComponentImpl implements Component {
  private final Type type;
  private final Status status;
  private final String name;
  private final String shortName;
  private final String dbKey;
  private final String key;
  private final String uuid;

  @CheckForNull
  private final String description;
  private final List<Component> children;
  @CheckForNull
  private final ProjectAttributes projectAttributes;
  private final ReportAttributes reportAttributes;
  @CheckForNull
  private final FileAttributes fileAttributes;

  private ComponentImpl(Builder builder) {
    this.type = builder.type;
    this.status = builder.status;
    this.dbKey = builder.dbKey;
    this.key = MoreObjects.firstNonNull(builder.key, builder.dbKey);
    this.name = builder.name;
    this.shortName = MoreObjects.firstNonNull(builder.shortName, builder.name).intern();
    this.description = builder.description;
    this.uuid = builder.uuid;
    this.projectAttributes = builder.projectAttributes;
    this.reportAttributes = builder.reportAttributes;
    this.fileAttributes = builder.fileAttributes;
    this.children = ImmutableList.copyOf(builder.children);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getDbKey() {
    return dbKey;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getShortName() {
    return this.shortName;
  }

  @Override
  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

  @Override
  public ProjectAttributes getProjectAttributes() {
    checkState(this.type == Type.PROJECT, "Only component of type PROJECT have a ProjectAttributes object");
    return this.projectAttributes;
  }

  @Override
  public ReportAttributes getReportAttributes() {
    return this.reportAttributes;
  }

  @Override
  public FileAttributes getFileAttributes() {
    checkState(this.type == Type.FILE, "Only component of type FILE have a FileAttributes object");
    return this.fileAttributes;
  }

  @Override
  public ProjectViewAttributes getProjectViewAttributes() {
    throw new IllegalStateException("Only component of type PROJECT_VIEW have a ProjectViewAttributes object");
  }

  @Override
  public SubViewAttributes getSubViewAttributes() {
    throw new IllegalStateException("Only component of type SUBVIEW have a SubViewAttributes object");
  }

  @Override
  public ViewAttributes getViewAttributes() {
    throw new IllegalStateException("Only component of type VIEW have a ViewAttributes object");
  }

  public static Builder builder(Type type) {
    return new Builder(type);
  }

  public static final class Builder {

    private static final String DB_KEY_CANNOT_BE_NULL = "DB key can't be null";
    private static final String KEY_CANNOT_BE_NULL = "Key can't be null";
    private static final String UUID_CANNOT_BE_NULL = "uuid can't be null";
    private static final String REPORT_ATTRIBUTES_CANNOT_BE_NULL = "reportAttributes can't be null";
    private static final String NAME_CANNOT_BE_NULL = "name can't be null";
    private static final String STATUS_CANNOT_BE_NULL = "status can't be null";

    private final Type type;
    private Status status;
    private ProjectAttributes projectAttributes;
    private ReportAttributes reportAttributes;
    private String uuid;
    private String dbKey;
    private String key;
    private String name;
    private String shortName;
    private String description;
    private FileAttributes fileAttributes;
    private final List<Component> children = new ArrayList<>();

    private Builder(Type type) {
      this.type = requireNonNull(type, "type can't be null");
    }

    public Builder setUuid(String s) {
      this.uuid = requireNonNull(s, UUID_CANNOT_BE_NULL);
      return this;
    }

    @CheckForNull
    public String getUuid() {
      return uuid;
    }

    public Builder setStatus(Status status) {
      this.status = requireNonNull(status, STATUS_CANNOT_BE_NULL);
      return this;
    }

    public Builder setDbKey(String s) {
      this.dbKey = requireNonNull(s, DB_KEY_CANNOT_BE_NULL);
      return this;
    }

    public Builder setKey(String key) {
      this.key = requireNonNull(key, KEY_CANNOT_BE_NULL);
      return this;
    }

    public Builder setName(String name) {
      this.name = abbreviate(requireNonNull(name, NAME_CANNOT_BE_NULL), MAX_COMPONENT_NAME_LENGTH);
      return this;
    }

    public Builder setShortName(String shortName) {
      this.shortName = abbreviate(requireNonNull(shortName, NAME_CANNOT_BE_NULL), MAX_COMPONENT_NAME_LENGTH);
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = abbreviate(trimToNull(description), MAX_COMPONENT_DESCRIPTION_LENGTH);
      return this;
    }

    public Builder setProjectAttributes(ProjectAttributes projectAttributes) {
      checkProjectAttributes(projectAttributes);
      this.projectAttributes = projectAttributes;
      return this;
    }

    public Builder setReportAttributes(ReportAttributes reportAttributes) {
      this.reportAttributes = requireNonNull(reportAttributes, REPORT_ATTRIBUTES_CANNOT_BE_NULL);
      return this;
    }

    public Builder setFileAttributes(@Nullable FileAttributes fileAttributes) {
      this.fileAttributes = fileAttributes;
      return this;
    }

    public Builder addChildren(List<Component> components) {
      for (Component component : components) {
        checkArgument(component.getType().isReportType());
      }
      this.children.addAll(components);
      return this;
    }

    public ComponentImpl build() {
      requireNonNull(reportAttributes, REPORT_ATTRIBUTES_CANNOT_BE_NULL);
      requireNonNull(uuid, UUID_CANNOT_BE_NULL);
      requireNonNull(dbKey, DB_KEY_CANNOT_BE_NULL);
      requireNonNull(name, NAME_CANNOT_BE_NULL);
      requireNonNull(status, STATUS_CANNOT_BE_NULL);
      checkProjectAttributes(this.projectAttributes);
      return new ComponentImpl(this);
    }

    private void checkProjectAttributes(@Nullable ProjectAttributes projectAttributes) {
      checkArgument(type != Type.PROJECT ^ projectAttributes != null, "ProjectAttributes must and can only be set for type PROJECT");
    }
  }

  @Override
  public String toString() {
    return "ComponentImpl{" +
      "type=" + type +
      ", status=" + status +
      ", name='" + name + '\'' +
      ", dbKey='" + dbKey + '\'' +
      ", key='" + key + '\'' +
      ", uuid='" + uuid + '\'' +
      ", description='" + description + '\'' +
      ", children=" + children +
      ", projectAttributes=" + projectAttributes +
      ", reportAttributes=" + reportAttributes +
      ", fileAttributes=" + fileAttributes +
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
    ComponentImpl component = (ComponentImpl) o;
    return uuid.equals(component.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
