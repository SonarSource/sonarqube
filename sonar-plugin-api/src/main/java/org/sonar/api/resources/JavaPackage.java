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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.utils.WildcardPattern;

/**
 * A class that represents a Java package in Sonar
 *
 * @since 1.10
 */
public class JavaPackage extends Resource {

  /**
   * Default package name for classes without package definition
   */
  public static final String DEFAULT_PACKAGE_NAME = "[default]";

  /**
   * Defaul constructor
   */
  public JavaPackage() {
    this(null);
  }

  /**
   * Creates a JavaPackage from its key. Will use DEFAULT_PACKAGE_NAME if key is null
   */
  public JavaPackage(String key) {
    setKey(StringUtils.defaultIfEmpty(StringUtils.trim(key), DEFAULT_PACKAGE_NAME));
  }

  /**
   * @return whether the JavaPackage key is the defult key
   */
  public boolean isDefault() {
    return StringUtils.equals(getKey(), DEFAULT_PACKAGE_NAME);
  }

  /**
   * {@inheritDoc}
   */
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return null;
  }

  /**
   * @return SCOPE_SPACE
   */
  public String getScope() {
    return Resource.SCOPE_SPACE;
  }

  /**
   * @return QUALIFIER_PACKAGE
   */
  public String getQualifier() {
    return Resource.QUALIFIER_PACKAGE;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return getKey();
  }

  /**
   * {@inheritDoc}
   */
  public Resource<?> getParent() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getLongName() {
    return null;
  }

  /**
   * @return Java
   */
  public Language getLanguage() {
    return Java.INSTANCE;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", getId())  
      .append("key", getKey())
      .toString();
  }
}