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
package org.sonar.api.resources;

import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputComponent;

/**
 * @since 1.10
 * @deprecated since 5.6 replaced by {@link InputComponent}
 */
@Deprecated
public abstract class Resource implements Serializable {

  /**
   * @deprecated since 2.6. Use Scopes.PROJECT.
   */
  @Deprecated
  public static final String SCOPE_SET = Scopes.PROJECT;

  /**
   * @deprecated since 2.6. Use Scopes.DIRECTORY.
   */
  @Deprecated
  public static final String SCOPE_SPACE = Scopes.DIRECTORY;

  /**
   * @deprecated since 2.6. Use Scopes.FILE.
   */
  @Deprecated
  public static final String SCOPE_ENTITY = Scopes.FILE;

  /**
   * @deprecated since 2.6. Use Qualifiers.VIEW.
   */
  @Deprecated
  public static final String QUALIFIER_VIEW = Qualifiers.VIEW;

  /**
   * @deprecated since 2.6. Use Qualifiers.SUBVIEW.
   */
  @Deprecated
  public static final String QUALIFIER_SUBVIEW = Qualifiers.SUBVIEW;

  /**
   * @deprecated since 2.6. Use Qualifiers.LIBRARY.
   */
  @Deprecated
  public static final String QUALIFIER_LIB = Qualifiers.LIBRARY;

  /**
   * @deprecated since 2.6. Use Qualifiers.PROJECT.
   */
  @Deprecated
  public static final String QUALIFIER_PROJECT = Qualifiers.PROJECT;

  /**
   * @deprecated since 2.6. Use Qualifiers.MODULE.
   */
  @Deprecated
  public static final String QUALIFIER_MODULE = Qualifiers.MODULE;

  /**
   * @deprecated since 2.6. Use Qualifiers.DIRECTORY.
   */
  @Deprecated
  public static final String QUALIFIER_DIRECTORY = Qualifiers.DIRECTORY;

  /**
   * @deprecated since 2.6. Use Qualifiers.FILE.
   */
  @Deprecated
  public static final String QUALIFIER_FILE = Qualifiers.FILE;

  private Integer id;

  private String key;

  private String uuid;

  private String path;

  private String effectiveKey;

  /**
   * @return the resource key
   */
  public final String getKey() {
    return key;
  }

  /**
   * Internal use only
   */
  public void setKey(String s) {
    this.key = s;
  }

  /**
   * @since 5.0
   */
  public final String getUuid() {
    return uuid;
  }

  /**
   * Internal use only
   */
  public void setUuid(String s) {
    this.uuid = s;
  }

  /**
   * @return the resource name
   */
  public abstract String getName();

  /**
   * @return the resource long name
   */
  public abstract String getLongName();

  /**
   * @return the resource description
   */
  public abstract String getDescription();

  /**
   * @return the language of the resource. Only {@link File}s may have a non null value.
   * @deprecated since 5.1 use {@link #language()}
   */
  @Deprecated
  @CheckForNull
  public abstract Language getLanguage();

  /**
   * @return the language of the resource. Only {@link File}s may have a non null value.
   */
  @CheckForNull
  public String language() {
    Language l = getLanguage();
    return l != null ? l.getKey() : null;
  }

  /**
   * @return the scope
   */
  public abstract String getScope();

  /**
   * The qualifier tells the type of the resource. For example, it can be a File, a Class, a Project, a Unit Test...
   *
   * @return the qualifier
   * @see org.sonar.api.resources.Qualifiers for the list of qualifiers
   * @see org.sonar.api.resources.ResourceUtils to find out if a resource if a class, a unit test,... from its qualifier
   */
  public abstract String getQualifier();

  /**
   * The parent is used to build the resources tree, for example for relations between files, directories and projects.
   * <p>
   * Return null if the parent is the current project (or module in case of multi-module).
   * 
   */
  @CheckForNull
  public abstract Resource getParent();

  /**
   * Check resource against an Ant pattern, like mypackag?/*Foo.java. It's used for example to match resource exclusions.
   *
   * @param antPattern Ant-like pattern (with **, * and ?). It includes file suffixes.
   * @return true if the resource matches the Ant pattern
   */
  public abstract boolean matchFilePattern(String antPattern);

  public final Integer getId() {
    return id;
  }

  /**
   * Internal use only
   */
  public Resource setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getPath() {
    return path;
  }

  public Resource setPath(@Nullable String path) {
    this.path = normalize(path);
    return this;
  }

  @CheckForNull
  protected static String normalize(@Nullable String path) {
    if (StringUtils.isBlank(path)) {
      return null;
    }
    String normalizedPath = path;
    normalizedPath = normalizedPath.replace('\\', '/');
    normalizedPath = StringUtils.trim(normalizedPath);
    if (Directory.SEPARATOR.equals(normalizedPath)) {
      return Directory.SEPARATOR;
    }
    normalizedPath = StringUtils.removeStart(normalizedPath, Directory.SEPARATOR);
    normalizedPath = StringUtils.removeEnd(normalizedPath, Directory.SEPARATOR);
    return normalizedPath;
  }

  public String getEffectiveKey() {
    return effectiveKey;
  }

  /**
   * Internal use only
   */
  public final Resource setEffectiveKey(String effectiveKey) {
    this.effectiveKey = effectiveKey;
    return this;
  }

  /**
   * @deprecated since 2.6.
   */
  @Deprecated
  public final boolean isExcluded() {
    return false;
  }

  /**
   * Internal use only
   *
   * @deprecated since 2.6 should use SensorContext#isExcluded(resource). It will make inheritance of Resource easier.
   */
  @Deprecated
  public final Resource setExcluded(boolean b) {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (getClass() != o.getClass()) {
      return false;
    }

    Resource resource = (Resource) o;
    return key.equals(resource.key);
  }

  @Override
  public int hashCode() {
    // For File and Directory using deprecatedKey, key can be null
    return key != null ? key.hashCode() : super.hashCode();
  }
}
