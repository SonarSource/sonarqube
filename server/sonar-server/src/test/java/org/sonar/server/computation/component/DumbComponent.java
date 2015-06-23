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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link Component} for unit tests.
 */
public class DumbComponent implements Component {

  private static final FileAttributes DEFAULT_FILE_ATTRIBUTES = new FileAttributes(false, null);

  public static final Component DUMB_PROJECT = builder(Type.PROJECT, 1).setKey("PROJECT_KEY").setUuid("PROJECT_UUID").setName("Project Name").setVersion("1.0-SNAPSHOT").build();

  private final Type type;
  private final int ref;
  private final String uuid, key, name, version;
  private final FileAttributes fileAttributes;
  private final List<Component> children;

  private DumbComponent(Builder builder) {
    this.type = builder.type;
    this.ref = builder.ref;
    this.uuid = builder.uuid;
    this.key = builder.key;
    this.name = builder.name;
    this.version = builder.version;
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
      throw new UnsupportedOperationException(String.format("Component uuid of ref '%d' has not be fed yet", ref));
    }
    return uuid;
  }

  @Override
  public String getKey() {
    if (key == null) {
      throw new UnsupportedOperationException(String.format("Component key of ref '%d' has not be fed yet", ref));
    }
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @CheckForNull
  public String getVersion() {
    return version;
  }

  @Override
  public int getRef() {
    return ref;
  }

  @Override
  public FileAttributes getFileAttributes() {
    checkState(this.type == Type.FILE, "Only component of type FILE can have a FileAttributes object");
    return this.fileAttributes;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DumbComponent that = (DumbComponent) o;
    return ref == that.ref;
  }

  @Override
  public int hashCode() {
    return ref;
  }

  public static Builder builder(Type type, int ref) {
    return new Builder(type, ref);
  }

  public static final class Builder {
    private final Type type;
    private final int ref;
    private String uuid, key, name, version;
    private FileAttributes fileAttributes;
    private final List<Component> children = new ArrayList<>();

    private Builder(Type type, int ref) {
      requireNonNull(type, "Component type must not be null");
      this.type = type;
      this.ref = ref;
    }

    public Builder setUuid(@Nullable String s) {
      this.uuid = s;
      return this;
    }

    public Builder setName(@Nullable String s) {
      this.name = s;
      return this;
    }

    public Builder setKey(@Nullable String s) {
      this.key = s;
      return this;
    }

    public Builder setVersion(@Nullable String s) {
      this.version = s;
      return this;
    }

    public Builder setFileAttributes(FileAttributes fileAttributes){
      checkState(type == Type.FILE, "Only Component of type File can have File attributes");
      this.fileAttributes = fileAttributes;
      return this;
    }

    public Builder addChildren(Component... c) {
      this.children.addAll(asList(c));
      return this;
    }

    public DumbComponent build() {
      return new DumbComponent(this);
    }
  }
}
