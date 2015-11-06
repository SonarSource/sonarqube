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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

@Immutable
public class ComponentImpl implements Component {
  private final Type type;
  private final String name;
  private final String key;
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
    this.key = builder.key;
    this.name = builder.name == null ? String.valueOf(builder.key) : builder.name;
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
  public String getUuid() {
    return uuid;
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
    throw new IllegalStateException("Only component of type PROJECT_VIEW have a FileAttributes object");
  }

  public static Builder builder(BatchReport.Component component) {
    return new Builder(component);
  }

  public static final class Builder {

    private final Type type;
    private final ReportAttributes reportAttributes;
    private String uuid;
    private String key;
    private String name;
    private String description;
    private FileAttributes fileAttributes;
    private final List<Component> children = new ArrayList<>();

    private Builder(BatchReport.Component component) {
      checkNotNull(component);
      this.type = convertType(component.getType());
      this.name = checkNotNull(component.getName());
      this.description = component.hasDescription() ? component.getDescription() : null;
      this.reportAttributes = createBatchAttributes(component);
      this.fileAttributes = createFileAttributes(component);
    }

    public Builder setUuid(String s) {
      this.uuid = checkNotNull(s);
      return this;
    }

    public Builder setKey(String s) {
      this.key = checkNotNull(s);
      return this;
    }

    public Builder addChildren(Component... c) {
      for (Component component : c) {
        checkArgument(component.getType().isReportType());
      }
      this.children.addAll(asList(c));
      return this;
    }

    public ComponentImpl build() {
      checkNotNull(key);
      checkNotNull(uuid);
      return new ComponentImpl(this);
    }

    private static ReportAttributes createBatchAttributes(BatchReport.Component component) {
      return ReportAttributes.newBuilder(component.getRef())
        .setVersion(component.hasVersion() ? component.getVersion() : null)
        .setPath(component.hasPath() ? component.getPath() : null)
        .build();
    }

    @CheckForNull
    private static FileAttributes createFileAttributes(BatchReport.Component component) {
      if (component.getType() != Constants.ComponentType.FILE) {
        return null;
      }

      return new FileAttributes(
        component.hasIsTest() && component.getIsTest(),
        component.hasLanguage() ? component.getLanguage() : null);
    }

    @VisibleForTesting
    static Type convertType(Constants.ComponentType type) {
      switch (type) {
        case PROJECT:
          return Type.PROJECT;
        case MODULE:
          return Type.MODULE;
        case DIRECTORY:
          return Type.DIRECTORY;
        case FILE:
          return Type.FILE;
        default:
          throw new IllegalArgumentException("Unsupported Constants.ComponentType value " + type);
      }
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
