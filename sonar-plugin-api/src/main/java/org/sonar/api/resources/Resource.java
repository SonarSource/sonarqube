/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * The interface to implement to create a resource in Sonar
 *
 * @since 1.10
 */
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
   * @deprecated since 2.6. Use Qualifiers.PACKAGE.
   */
  @Deprecated
  public static final String QUALIFIER_PACKAGE = Qualifiers.PACKAGE;

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

  /**
   * @deprecated since 2.6. Use Qualifiers.CLASS.
   */
  @Deprecated
  public static final String QUALIFIER_CLASS = Qualifiers.CLASS;

  /**
   * @deprecated since 2.6. Use Qualifiers.FIELD.
   */
  @Deprecated
  public static final String QUALIFIER_FIELD = Qualifiers.FIELD;

  /**
   * @deprecated since 2.6. Use Qualifiers.METHOD.
   */
  @Deprecated
  public static final String QUALIFIER_METHOD = Qualifiers.METHOD;

  /**
   * @deprecated since 2.6. Use Qualifiers.UNIT_TEST_FILE.
   */
  @Deprecated
  public static final String QUALIFIER_UNIT_TEST_CLASS = Qualifiers.UNIT_TEST_FILE;

  private Integer id = null;

  private String key = null;

  private String deprecatedKey = null;

  private String path = null;

  private String effectiveKey = null;

  private boolean isExcluded = false;

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
   * @return the resource deprecated key. Should not be used except to deal with backward compatibility.
   * @since 4.2
   */
  public final String getDeprecatedKey() {
    return deprecatedKey;
  }

  /**
   * For internal use only
   */
  public void setDeprecatedKey(String s) {
    this.deprecatedKey = s;
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
   * @return the language
   */
  public abstract Language getLanguage();

  /**
   * @return the scope
   */
  public abstract String getScope();

  /**
   * The qualifier tells the type of the resource. For example, it can be a File, a Class, a Project, a Unit Test...
   *
   * @return the qualifier
   *
   * @see org.sonar.api.resources.Qualifiers for the list of qualifiers
   * @see org.sonar.api.resources.ResourceUtils to find out if a resource if a class, a unit test,... from its qualifier
   */
  public abstract String getQualifier();

  /**
   * The parent is used to build the resources tree, for example for relations between classes, packages and projects.
   * <p>
   * Return null if the parent is the project.
   * </p>
   */
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
   * @deprecated since 2.6 should use SensorContext#isExcluded(resource). It will make inheritance of Resource easier.
   */
  @Deprecated
  public final boolean isExcluded() {
    return isExcluded;
  }

  /**
   * Internal use only
   * @deprecated since 2.6 should use SensorContext#isExcluded(resource). It will make inheritance of Resource easier.
   */
  @Deprecated
  public final Resource setExcluded(boolean b) {
    isExcluded = b;
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
    // JavaPackage and Directory are considered equivalent since 4.2
    if (getClass() != o.getClass() && !(this instanceof JavaPackage && o instanceof JavaPackage)) {
      return false;
    }

    Resource resource = (Resource) o;
    if (key != null) {
      return key.equals(resource.key);
    } else {
      return resource.key == null && deprecatedKey.equals(resource.deprecatedKey);
    }
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : deprecatedKey.hashCode();
  }
}
