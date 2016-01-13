/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

/**
 * A class that represents a Java package in Sonar
 *
 * @since 1.10
 * @deprecated since 4.2 use {@link Directory} instead
 */
@Deprecated
public class JavaPackage extends Resource {

  public static final String DEFAULT_PACKAGE_NAME = "[default]";

  public JavaPackage() {
    // For testing
  }

  public JavaPackage(String deprecatedKey) {
    throw unsupported();
  }

  public boolean isDefault() {
    throw unsupported();
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    throw unsupported();
  }

  @Override
  public String getDescription() {
    throw unsupported();
  }

  @Override
  public String getScope() {
    throw unsupported();
  }

  @Override
  public String getQualifier() {
    throw unsupported();
  }

  @Override
  public String getName() {
    throw unsupported();
  }

  @Override
  public Resource getParent() {
    throw unsupported();
  }

  @Override
  public String getLongName() {
    throw unsupported();
  }

  @Override
  public Language getLanguage() {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    throw new UnsupportedOperationException("Not supported since v4.2. See http://redirect.sonarsource.com/doc/api-changes.html");
  }
}
