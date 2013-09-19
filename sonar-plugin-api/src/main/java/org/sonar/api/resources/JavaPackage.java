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
import org.apache.commons.lang.builder.ToStringBuilder;

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
   * Default constructor
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
   * @return whether the JavaPackage key is the default key
   */
  public boolean isDefault() {
    return StringUtils.equals(getKey(), DEFAULT_PACKAGE_NAME);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return null;
  }

  /**
   * @return SCOPE_SPACE
   */
  @Override
  public String getScope() {
    return Scopes.DIRECTORY;
  }

  /**
   * @return QUALIFIER_PACKAGE
   */
  @Override
  public String getQualifier() {
    return Qualifiers.PACKAGE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return getKey();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Resource<?> getParent() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLongName() {
    return null;
  }

  /**
   * @return Java
   */
  @Override
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
