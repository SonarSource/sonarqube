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
package org.sonar.api.resources;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.util.List;

/**
 * A class that represents a Java class. This class can either be a Test class or source class
 *
 * @since 1.10
 * @deprecated since 4.2 use {@link org.sonar.api.resources.File}. See
 * http://docs.codehaus.org/display/SONAR/API+Changes for more details
 */
@Deprecated
public class JavaFile extends Resource {

  @VisibleForTesting
  JavaFile() {
  }

  public JavaFile(String packageName, String className) {
    throw unsupported();
  }

  public JavaFile(String packageKey, String className, boolean unitTest) {
    throw unsupported();
  }

  public JavaFile(String deprecatedKey) {
    throw unsupported();
  }

  public JavaFile(String deprecatedKey, boolean unitTest) {
    throw unsupported();
  }

  @Override
  public JavaPackage getParent() {
    throw unsupported();
  }

  @Override
  public String getDescription() {
    throw unsupported();
  }

  @Override
  public Language getLanguage() {
    throw unsupported();
  }

  @Override
  public String getName() {
    throw unsupported();
  }

  @Override
  public String getLongName() {
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

  public boolean isUnitTest() {
    throw unsupported();
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    throw unsupported();
  }

  public static JavaFile fromIOFile(File file, Project module, boolean unitTest) {
    throw unsupported();
  }

  public static JavaFile fromRelativePath(String relativePath, boolean unitTest) {
    throw unsupported();
  }

  public static JavaFile fromIOFile(File file, List<File> sourceDirs, boolean unitTest) {
    throw unsupported();
  }

  public static JavaFile fromAbsolutePath(String path, List<File> sourceDirs, boolean unitTest) {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    throw new UnsupportedOperationException("Not supported since v4.2. See http://redirect.sonarsource.com/doc/api-changes.html");
  }

}
