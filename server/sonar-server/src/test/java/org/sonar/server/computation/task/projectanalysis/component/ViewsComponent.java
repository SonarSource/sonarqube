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
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link Component} to unit test views components.
 */
public class ViewsComponent implements Component {
  private final Type type;
  private final String key;
  private final String uuid;
  private final String name;
  private final String description;
  private final List<Component> children;
  private final ProjectViewAttributes projectViewAttributes;
  private final SubViewAttributes subViewAttributes;
  private final ViewAttributes viewAttributes;

  private ViewsComponent(Type type, String key, @Nullable String uuid, @Nullable String name, @Nullable String description,
    List<Component> children,
    @Nullable ProjectViewAttributes projectViewAttributes, @Nullable SubViewAttributes subViewAttributes, @Nullable ViewAttributes viewAttributes) {
    checkArgument(type.isViewsType(), "Component type must be a Views type");
    this.type = type;
    this.key = requireNonNull(key);
    this.uuid = uuid;
    this.name = name;
    this.description = description;
    this.children = ImmutableList.copyOf(children);
    this.projectViewAttributes = projectViewAttributes;
    this.subViewAttributes = subViewAttributes;
    this.viewAttributes = viewAttributes;
  }

  public static Builder builder(Type type, String key) {
    return new Builder(type, key);
  }

  public static Builder builder(Type type, int key) {
    return new Builder(type, String.valueOf(key));
  }

  public static final class Builder {
    private final Type type;
    private final String key;
    private String uuid;
    private String name;
    private String description;
    private List<Component> children = new ArrayList<>();
    private ProjectViewAttributes projectViewAttributes;
    private SubViewAttributes subViewAttributes;
    private ViewAttributes viewAttributes;

    private Builder(Type type, String key) {
      this.type = type;
      this.key = key;
    }

    public Builder setUuid(@Nullable String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setChildren(List<Component> children) {
      this.children = children;
      return this;
    }

    public Builder setProjectViewAttributes(@Nullable ProjectViewAttributes projectViewAttributes) {
      this.projectViewAttributes = projectViewAttributes;
      return this;
    }

    public Builder setSubViewAttributes(@Nullable SubViewAttributes subViewAttributes) {
      this.subViewAttributes = subViewAttributes;
      return this;
    }

    public Builder setViewAttributes(@Nullable ViewAttributes viewAttributes) {
      this.viewAttributes = viewAttributes;
      return this;
    }

    public Builder addChildren(Component... c) {
      for (Component viewsComponent : c) {
        checkArgument(viewsComponent.getType().isViewsType());
      }
      this.children.addAll(asList(c));
      return this;
    }

    public ViewsComponent build() {
      return new ViewsComponent(type, key, uuid, name, description, children, projectViewAttributes, subViewAttributes, viewAttributes);
    }
  }

  @Override
  public Type getType() {
    return type;
  }
  
  @Override
  public Status getStatus() {
    return Status.UNAVAILABLE;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getKey() {
    return key;
  }

  /**
   * Views has no branch feature, the public key is the same as the key
   */
  @Override
  public String getPublicKey() {
    return getKey();
  }

  @Override
  public String getName() {
    checkState(this.name != null, "No name has been set");
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
    throw new IllegalStateException("A component of type " + type + " does not have report attributes");
  }

  @Override
  public FileAttributes getFileAttributes() {
    throw new IllegalStateException("A component of type " + type + " does not have file attributes");
  }

  @Override
  public ProjectViewAttributes getProjectViewAttributes() {
    checkState(this.type != Type.PROJECT_VIEW || this.projectViewAttributes != null, "A ProjectViewAttribute object should have been set");
    return this.projectViewAttributes;
  }

  @Override
  public SubViewAttributes getSubViewAttributes() {
    checkState(this.type != Type.SUBVIEW || this.subViewAttributes != null, "A SubViewAttributes object should have been set");
    return this.subViewAttributes;
  }

  @Override
  public ViewAttributes getViewAttributes() {
    checkState(this.type != Type.VIEW || this.viewAttributes != null, "A ViewAttributes object should have been set");
    return viewAttributes;
  }

  @Override
  public String toString() {
    return "ViewsComponent{" +
      "type=" + type +
      ", key='" + key + '\'' +
      ", uuid='" + uuid + '\'' +
      ", name='" + name + '\'' +
      ", children=" + children +
      ", projectViewAttributes=" + projectViewAttributes +
      ", subViewAttributes=" + subViewAttributes +
      ", viewAttributes=" + viewAttributes +
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
    ViewsComponent that = (ViewsComponent) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
