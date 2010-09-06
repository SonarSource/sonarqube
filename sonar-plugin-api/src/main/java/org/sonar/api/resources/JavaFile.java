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
  private JavaPackage parent = null;

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
    if (className==null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    if (className.indexOf('$') >= 0) {
      throw new IllegalArgumentException("Java inner classes are not supported : " + className);
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
    if (key==null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    if (key != null && key.indexOf('$') >= 0) {
      throw new IllegalArgumentException("Java inner classes are not supported : " + key);
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
  public JavaPackage getParent() {
    if (parent == null) {
      parent = new JavaPackage(packageKey);
    }
    return parent;
  }

  /**
   * @return null
   */
  public String getDescription() {
    return null;
  }

  /**
   * @return Java
   */
  public Language getLanguage() {
    return Java.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return filename;
  }

  /**
   * {@inheritDoc}
   */
  public String getLongName() {
    return longName;
  }

  /**
   * @return SCOPE_ENTITY
   */
  public String getScope() {
    return Resource.SCOPE_ENTITY;
  }

  /**
   * @return QUALIFIER_UNIT_TEST_CLASS or QUALIFIER_CLASS depending whether it is a unit test class
   */
  public String getQualifier() {
    return unitTest ? Resource.QUALIFIER_UNIT_TEST_CLASS : Resource.QUALIFIER_CLASS;
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
  public boolean matchFilePattern(String antPattern) {
    if (unitTest) {
      return false;
    }
    String fileKey = getKey();
    if (!fileKey.endsWith(".java")) {
      fileKey += ".java";
    }
    if (StringUtils.substringAfterLast(antPattern, "/").indexOf(".")<0) {
      antPattern += ".*";
    }
    WildcardPattern matcher = WildcardPattern.create(antPattern, ".");
    return matcher.match(fileKey);
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
    String relativePath = DefaultProjectFileSystem.getRelativePath(file, sourceDirs);
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
    return new ToStringBuilder(this)
        .append("key", getKey())
        .append("package", packageKey)
        .append("longName", longName)
        .append("unitTest", unitTest)
        .toString();
  }
}
