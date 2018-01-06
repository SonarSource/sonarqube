/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.trimToNull;

@Immutable
public class ComponentImpl implements Component {
  private final Type type;
  private final Status status;
  private final String name;
  private final String key;
  private final String publicKey;
  private final String uuid;

  @CheckForNull
  private final String description;
  private final List<Component> children;
  @CheckForNull
  private final ReportAttributes reportAttributes;
  @CheckForNull
  private final FileAttributes fileAttributes;

  private ComponentImpl(Builder builder) {
    this.type = builder.type;
    this.status = builder.status;
    this.key = builder.key;
    this.publicKey = builder.publicKey;
    this.name = builder.name;
    this.description = builder.description;
    this.uuid = builder.uuid;
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
  public String getKey() {
    return key;
  }

  @Override
  public String getPublicKey() {
    return publicKey;
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

  @Override
  public List<Component> getChildren() {
    return children;
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

    private static final String KEY_CANNOT_BE_NULL = "key can't be null";
    private static final String UUID_CANNOT_BE_NULL = "uuid can't be null";
    private static final String REPORT_ATTRIBUTES_CANNOT_BE_NULL = "reportAttributes can't be null";
    private static final String NAME_CANNOT_BE_NULL = "name can't be null";
    private static final String STATUS_CANNOT_BE_NULL = "status can't be null";

    private final Type type;
    private Status status;
    private ReportAttributes reportAttributes;
    private String uuid;
    private String key;
    private String publicKey;
    private String name;
    private String description;
    private FileAttributes fileAttributes;
    private final List<Component> children = new ArrayList<>();

    private Builder(Type type) {
      this.type = requireNonNull(type, "type can't be null");
    }

    public Builder setReportAttributes(ReportAttributes reportAttributes) {
      this.reportAttributes = requireNonNull(reportAttributes, REPORT_ATTRIBUTES_CANNOT_BE_NULL);
      return this;
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

    public Builder setKey(String s) {
      this.key = requireNonNull(s, KEY_CANNOT_BE_NULL);
      return this;
    }

    public Builder setPublicKey(String publicKey) {
      this.publicKey = requireNonNull(publicKey);
      return this;
    }

    public Builder setName(String name) {
      this.name = requireNonNull(name, NAME_CANNOT_BE_NULL);
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = trimToNull(description);
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
      requireNonNull(key, KEY_CANNOT_BE_NULL);
      requireNonNull(name, NAME_CANNOT_BE_NULL);
      requireNonNull(status, STATUS_CANNOT_BE_NULL);
      return new ComponentImpl(this);
    }
  }

  @Override
  public String toString() {
    return "ComponentImpl{" +
      "key='" + key + '\'' +
      ", type=" + type +
      ", uuid='" + uuid + '\'' +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", fileAttributes=" + fileAttributes +
      ", reportAttributes=" + reportAttributes +
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
