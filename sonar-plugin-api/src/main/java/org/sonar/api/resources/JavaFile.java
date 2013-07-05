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
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;
import java.util.List;

/**
 * A class that represents a Java class. This class can either be a Test class or source class
 *
 * @since 1.10
 */
public class JavaFile extends Resource<JavaPackage> {

  private String filename;
  private String longName;
  private String packageKey;
  private boolean unitTest;
  private JavaPackage parent;

  /**
   * Creates a JavaFile that is not a class of test based on package and file names
   */
  public JavaFile(String packageName, String className) {
    this(packageName, className, false);
  }

  /**
   * Creates a JavaFile that can be of any type based on package and file names
   *
   * @param unitTest whether it is a unit test file or a source file
   */
  public JavaFile(String packageKey, String className, boolean unitTest) {
    if (className == null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    this.filename = StringUtils.trim(className);
    String key;
    if (StringUtils.isBlank(packageKey)) {
      this.packageKey = JavaPackage.DEFAULT_PACKAGE_NAME;
      this.longName = this.filename;
      key = new StringBuilder().append(this.packageKey).append(".").append(this.filename).toString();
    } else {
      this.packageKey = packageKey.trim();
      key = new StringBuilder().append(this.packageKey).append(".").append(this.filename).toString();
      this.longName = key;
    }
    setKey(key);
    this.unitTest = unitTest;
  }

  /**
   * Creates a source file from its key
   */
  public JavaFile(String key) {
    this(key, false);
  }

  /**
   * Creates any JavaFile from its key
   *
   * @param unitTest whether it is a unit test file or a source file
   */
  public JavaFile(String key, boolean unitTest) {
    if (key == null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    String realKey = StringUtils.trim(key);
    this.unitTest = unitTest;

    if (realKey.contains(".")) {
      this.filename = StringUtils.substringAfterLast(realKey, ".");
      this.packageKey = StringUtils.substringBeforeLast(realKey, ".");
      this.longName = realKey;

    } else {
      this.filename = realKey;
      this.longName = realKey;
      this.packageKey = JavaPackage.DEFAULT_PACKAGE_NAME;
      realKey = new StringBuilder().append(JavaPackage.DEFAULT_PACKAGE_NAME).append(".").append(realKey).toString();
    }
    setKey(realKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JavaPackage getParent() {
    if (parent == null) {
      parent = new JavaPackage(packageKey);
    }
    return parent;

  }

  /**
   * @return null
   */
  @Override
  public String getDescription() {
    return null;
  }

  /**
   * @return Java
   */
  @Override
  public Language getLanguage() {
    return Java.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return filename;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLongName() {
    return longName;
  }

  /**
   * @return SCOPE_ENTITY
   */
  @Override
  public String getScope() {
    return Scopes.FILE;
  }

  /**
   * @return QUALIFIER_UNIT_TEST_CLASS or QUALIFIER_CLASS depending whether it is a unit test class
   */
  @Override
  public String getQualifier() {
    return unitTest ? Qualifiers.UNIT_TEST_FILE : Qualifiers.CLASS;
  }

  /**
   * @return whether the JavaFile is a unit test class or not
   */
  public boolean isUnitTest() {
    return unitTest;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matchFilePattern(String antPattern) {
    String fileKey = getKey();
    if (!fileKey.endsWith(".java")) {
      fileKey += ".java";
    }
    // Add wildcard extension if not provided
    if ((antPattern.contains("/") && StringUtils.substringAfterLast(antPattern, "/").indexOf('.') < 0) || antPattern.indexOf('.') < 0) {
      antPattern += ".*";
    }
    String noPackagePrefix = JavaPackage.DEFAULT_PACKAGE_NAME + ".";
    if (fileKey.startsWith(noPackagePrefix)) {
      fileKey = fileKey.substring(noPackagePrefix.length());
    }
    WildcardPattern matcher = WildcardPattern.create(antPattern, ".");
    return matcher.match(fileKey);
  }

  public static JavaFile fromRelativePath(String relativePath, boolean unitTest) {
    if (relativePath != null) {
      String pacname = null;
      String classname = relativePath;

      if (relativePath.indexOf('/') >= 0) {
        pacname = StringUtils.substringBeforeLast(relativePath, "/");
        pacname = StringUtils.replace(pacname, "/", ".");
        classname = StringUtils.substringAfterLast(relativePath, "/");
      }
      classname = StringUtils.substringBeforeLast(classname, ".");
      return new JavaFile(pacname, classname, unitTest);
    }
    return null;
  }

  /**
   * Creates a JavaFile from a file in the source directories
   *
   * @return the JavaFile created if exists, null otherwise
   */
  public static JavaFile fromIOFile(File file, List<File> sourceDirs, boolean unitTest) {
    if (file == null || !StringUtils.endsWithIgnoreCase(file.getName(), ".java")) {
      return null;
    }
    PathResolver.RelativePath relativePath = new PathResolver().relativePath(sourceDirs, file);
    if (relativePath != null) {
      return fromRelativePath(relativePath.path(), unitTest);
    }
    return null;
  }

  /**
   * Shortcut to fromIOFile with an abolute path
   */
  public static JavaFile fromAbsolutePath(String path, List<File> sourceDirs, boolean unitTest) {
    if (path == null) {
      return null;
    }
    return fromIOFile(new File(path), sourceDirs, unitTest);
  }

  @Override
  public String toString() {
    return getKey();
  }

}
