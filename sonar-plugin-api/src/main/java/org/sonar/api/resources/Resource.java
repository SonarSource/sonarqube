/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

/**
 * The interface to implement to create a resource in Sonar
 *
 * @since 1.10
 */
public abstract class Resource<PARENT extends Resource> {

  public static final String SCOPE_SET = "PRJ";
  public static final String SCOPE_SPACE = "DIR";
  public static final String SCOPE_ENTITY = "FIL";
  
  /**
   * Use SCOPE_SET instead
   */
  @Deprecated
  public static final String SCOPE_PROJECT = SCOPE_SET;

  /**
   * Use SCOPE_SPACE instead
   */
  @Deprecated
  public static final String SCOPE_DIRECTORY = SCOPE_SPACE;

  /**
   * Use SCOPE_ENTITY instead
   */
  @Deprecated
  public static final String SCOPE_FILE = SCOPE_ENTITY;


  public static final String QUALIFIER_VIEW = "VW";
  public static final String QUALIFIER_SUBVIEW = "SVW";
  public static final String QUALIFIER_LIB = "LIB";
  public static final String QUALIFIER_PROJECT = "TRK";
  public static final String QUALIFIER_MODULE = "BRC";
  public static final String QUALIFIER_PACKAGE = "PAC";
  public static final String QUALIFIER_DIRECTORY = "DIR";
  public static final String QUALIFIER_FILE = "FIL";
  public static final String QUALIFIER_CLASS = "CLA";
  public static final String QUALIFIER_FIELD = "FLD";
  public static final String QUALIFIER_METHOD = "MET";
  public static final String QUALIFIER_UNIT_TEST_CLASS = "UTS";

  /**
   * Use QUALIFIER_PROJECT instead
   */
  @Deprecated
  public static final String QUALIFIER_PROJECT_TRUNK = QUALIFIER_PROJECT;

  /**
   * Use QUALIFIER_MODULE instead
   */
  @Deprecated
  public static final String QUALIFIER_PROJECT_BRANCH = QUALIFIER_MODULE;

  private Integer id = null;

  private String key = null;

  private String effectiveKey = null;

  private boolean isExcluded = false;


  /**
   * @return the resource key
   */
  public final String getKey() {
    return key;
  }

  protected void setKey(String s) {
    this.key = s;
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
   * @return the qualifier
   */
  public abstract String getQualifier();

  /**
   * The parent is used to build the resources tree, for example for relations between classes, packages and projects.
   * <p>Return null if the parent is the project.</p>
   */
  public abstract PARENT getParent();

  /**
   * Check resource against an Ant pattern, like mypackag?/*Foo.java. It's used for example
   * to match resource exclusions.
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

  public final String getEffectiveKey() {
    return effectiveKey;
  }

  /**
   * Internal use only
   */
  public final Resource setEffectiveKey(String effectiveKey) {
    this.effectiveKey = effectiveKey;
    return this;
  }

  public final boolean isExcluded() {
    return isExcluded;
  }

  /**
   * Internal use only
   */
  public final Resource setExcluded(boolean b) {
    isExcluded = b;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Resource resource = (Resource) o;
    return key.equals(resource.key);

  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
