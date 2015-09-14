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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;

public class ComponentImpl implements Component {
  private final Type type;
  private final String name;
  @CheckForNull
  private final String description;
  private final List<Component> children;
  @CheckForNull
  private final ReportAttributes reportAttributes;
  @CheckForNull
  private final FileAttributes fileAttributes;

  // Mutable values
  private String key;
  private String uuid;

  public ComponentImpl(BatchReport.Component component, @Nullable Iterable<Component> children) {
    this.type = convertType(component.getType());
    this.name = checkNotNull(component.getName());
    this.description = component.hasDescription() ? component.getDescription() : null;
    this.reportAttributes = createBatchAttributes(component);
    this.fileAttributes = createFileAttributes(component);
    this.children = children == null ? Collections.<Component>emptyList() : copyOf(filter(children, notNull()));
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

  public static Type convertType(Constants.ComponentType type) {
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

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getUuid() {
    if (uuid == null) {
      throw new UnsupportedOperationException(String.format("Component uuid of ref '%s' has not be fed yet", this.reportAttributes.getRef()));
    }
    return uuid;
  }

  public ComponentImpl setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @Override
  public String getKey() {
    if (key == null) {
      throw new UnsupportedOperationException(String.format("Component key of ref '%s' has not be fed yet", this.reportAttributes.getRef()));
    }
    return key;
  }

  public ComponentImpl setKey(String key) {
    this.key = key;
    return this;
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
}
