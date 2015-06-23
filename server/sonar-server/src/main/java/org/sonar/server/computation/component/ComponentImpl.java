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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;

public class ComponentImpl implements Component {
  private final Type type;
  private final int ref;
  private final String name;
  private final String version;
  @CheckForNull
  private final FileAttributes fileAttributes;
  private final List<Component> children;

  // Mutable values
  private String key;
  private String uuid;

  public ComponentImpl(BatchReport.Component component, @Nullable Iterable<Component> children) {
    this.ref = component.getRef();
    this.type = convertType(component.getType());
    this.name = component.getName();
    this.version = component.hasVersion() ? component.getVersion() : null;
    this.fileAttributes = createFileAttributes(component);
    this.children = children == null ? Collections.<Component>emptyList() : copyOf(filter(children, notNull()));
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
  public int getRef() {
    return ref;
  }

  @Override
  public String getUuid() {
    if (uuid == null) {
      throw new UnsupportedOperationException(String.format("Component uuid of ref '%s' has not be fed yet", getRef()));
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
      throw new UnsupportedOperationException(String.format("Component key of ref '%s' has not be fed yet", getRef()));
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

  public ComponentImpl setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public FileAttributes getFileAttributes() {
    checkState(this.type == Type.FILE, "Only component of type FILE have a FileAttributes object");
    return this.fileAttributes;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

}
