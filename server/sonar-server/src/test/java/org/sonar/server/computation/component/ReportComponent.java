/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

/**
 * Implementation of {@link Component} to unit test report components.
 */
public class ReportComponent implements Component {

  private static final FileAttributes DEFAULT_FILE_ATTRIBUTES = new FileAttributes(false, null);

  public static final Component DUMB_PROJECT = builder(Type.PROJECT, 1).setKey("PROJECT_KEY").setUuid("PROJECT_UUID").setName("Project Name").setVersion("1.0-SNAPSHOT").build();

  private final Type type;
  private final String name;
  @CheckForNull
  private final String description;
  private final String key;
  private final String uuid;
  private final ReportAttributes reportAttributes;
  private final FileAttributes fileAttributes;
  private final List<Component> children;

  private ReportComponent(Builder builder) {
    this.type = builder.type;
    this.key = builder.key;
    this.name = builder.name == null ? String.valueOf(builder.key) : builder.name;
    this.description = builder.description;
    this.uuid = builder.uuid;
    this.reportAttributes = ReportAttributes.newBuilder(builder.ref)
      .setVersion(builder.version)
      .setPath(builder.path)
      .build();
    this.fileAttributes = builder.fileAttributes == null ? DEFAULT_FILE_ATTRIBUTES : builder.fileAttributes;
    this.children = ImmutableList.copyOf(builder.children);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getUuid() {
    if (uuid == null) {
      throw new UnsupportedOperationException(String.format("Component uuid of ref '%d' has not be fed yet", this.reportAttributes.getRef()));
    }
    return uuid;
  }

  @Override
  public String getKey() {
    if (key == null) {
      throw new UnsupportedOperationException(String.format("Component key of ref '%d' has not be fed yet", this.reportAttributes.getRef()));
    }
    return key;
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
    checkState(this.type == Type.FILE, "Only component of type FILE can have a FileAttributes object");
    return this.fileAttributes;
  }

  @Override
  public ProjectViewAttributes getProjectViewAttributes() {
    throw new IllegalStateException("Only component of type PROJECT_VIEW can have a ProjectViewAttributes object");
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportComponent that = (ReportComponent) o;
    return reportAttributes.getRef() == that.reportAttributes.getRef();
  }

  @Override
  public int hashCode() {
    return this.reportAttributes.getRef();
  }

  @Override
  public String toString() {
    return "ReportComponent{" +
      "ref=" + this.reportAttributes.getRef() +
      ", key='" + key + '\'' +
      ", type=" + type +
      '}';
  }

  public static Builder builder(Type type, int ref) {
    return new Builder(type, ref).setKey("key_" + ref).setUuid("uuid_" + ref);
  }

  public static final class Builder {
    private final Type type;
    private final int ref;
    private String uuid;
    private String key;
    private String name;
    private String version;
    private String description;
    private String path;
    private FileAttributes fileAttributes;
    private final List<Component> children = new ArrayList<>();

    private Builder(Type type, int ref) {
      checkArgument(type.isReportType(), "Component type must not be a report type");
      this.type = type;
      this.ref = ref;
    }

    public Builder setUuid(String s) {
      this.uuid = Objects.requireNonNull(s);
      return this;
    }

    public Builder setName(@Nullable String s) {
      this.name = s;
      return this;
    }

    public Builder setKey(String s) {
      this.key = Objects.requireNonNull(s);
      return this;
    }

    public Builder setVersion(@Nullable String s) {
      this.version = s;
      return this;
    }

    public Builder setFileAttributes(FileAttributes fileAttributes) {
      checkState(type == Type.FILE, "Only Component of type File can have File attributes");
      this.fileAttributes = fileAttributes;
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = description;
      return this;
    }

    public Builder setPath(@Nullable String path) {
      this.path = path;
      return this;
    }

    public Builder addChildren(Component... c) {
      for (Component component : c) {
        checkArgument(component.getType().isReportType());
      }
      this.children.addAll(asList(c));
      return this;
    }

    public ReportComponent build() {
      return new ReportComponent(this);
    }
  }
}
