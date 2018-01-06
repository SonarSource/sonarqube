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
package org.sonar.api.ce.measure.test;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Component;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

@Immutable
public class TestComponent implements Component {

  private final String key;

  private final Type type;

  @CheckForNull
  private final FileAttributes fileAttributes;

  public TestComponent(String key, Type type, @Nullable FileAttributes fileAttributes) {
    this.key = requireNonNull(key, "Key cannot be null");
    this.type = requireNonNull(type, "Type cannot be null");
    this.fileAttributes = checkFileAttributes(fileAttributes);
  }

  @CheckForNull
  private FileAttributes checkFileAttributes(@Nullable FileAttributes fileAttributes) {
    if (fileAttributes == null && type == Type.FILE) {
      throw new IllegalArgumentException("Component of type FILE must have a FileAttributes object");
    } else if (fileAttributes != null && type != Type.FILE) {
      throw new IllegalArgumentException("Only component of type FILE have a FileAttributes object");
    }
    return fileAttributes;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public FileAttributes getFileAttributes() {
    checkState(this.type == Component.Type.FILE, "Only component of type FILE have a FileAttributes object");
    return fileAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TestComponent component = (TestComponent) o;

    return key.equals(component.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return "ComponentImpl{" +
      "key=" + key +
      ", type='" + type + '\'' +
      ", fileAttributes=" + fileAttributes +
      '}';
  }

  @Immutable
  public static class FileAttributesImpl implements FileAttributes {

    private final boolean unitTest;
    private final String languageKey;

    public FileAttributesImpl(@Nullable String languageKey, boolean unitTest) {
      this.languageKey = languageKey;
      this.unitTest = unitTest;
    }

    @Override
    public boolean isUnitTest() {
      return unitTest;
    }

    @Override
    @CheckForNull
    public String getLanguageKey() {
      return languageKey;
    }

    @Override
    public String toString() {
      return "FileAttributesImpl{" +
        "languageKey='" + languageKey + '\'' +
        ", unitTest=" + unitTest +
        '}';
    }
  }
}

