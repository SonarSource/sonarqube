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
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;
import java.util.List;

/**
 * A class that represents a Java class. This class can either be a Test class or source class
 *
 * @since 1.10
 */
public class JavaFile extends Resource {

  private static final String JAVA_SUFFIX = ".java";
  private static final String JAV_SUFFIX = ".jav";
  private String className;
  private String filename;
  private String fullyQualifiedName;
  private String packageFullyQualifiedName;
  private boolean unitTest;
  private JavaPackage parent;

  private JavaFile() {
    // Default constructor
  }

  /**
   * Creates a JavaFile that is not a class of test based on package and file names
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public JavaFile(String packageName, String className) {
    this(packageName, className, false);
  }

  /**
   * Creates a JavaFile that can be of any type based on package and file names
   *
   * @param unitTest whether it is a unit test file or a source file
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public JavaFile(String packageKey, String className, boolean unitTest) {
    if (className == null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    this.className = StringUtils.trim(className);
    String deprecatedKey;
    if (StringUtils.isBlank(packageKey)) {
      this.packageFullyQualifiedName = JavaPackage.DEFAULT_PACKAGE_NAME;
      this.fullyQualifiedName = this.className;
      deprecatedKey = new StringBuilder().append(this.packageFullyQualifiedName).append(".").append(this.className).toString();
    } else {
      this.packageFullyQualifiedName = packageKey.trim();
      deprecatedKey = new StringBuilder().append(this.packageFullyQualifiedName).append(".").append(this.className).toString();
      this.fullyQualifiedName = deprecatedKey;
    }
    setDeprecatedKey(deprecatedKey);
    this.unitTest = unitTest;
  }

  /**
   * Creates a source file from its key
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public JavaFile(String key) {
    this(key, false);
  }

  /**
   * Creates any JavaFile from its key
   *
   * @param unitTest whether it is a unit test file or a source file
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public JavaFile(String key, boolean unitTest) {
    if (key == null) {
      throw new IllegalArgumentException("Java filename can not be null");
    }
    String realKey = StringUtils.trim(key);
    this.unitTest = unitTest;

    if (realKey.contains(".")) {
      this.className = StringUtils.substringAfterLast(realKey, ".");
      this.packageFullyQualifiedName = StringUtils.substringBeforeLast(realKey, ".");
      this.fullyQualifiedName = realKey;

    } else {
      this.className = realKey;
      this.fullyQualifiedName = realKey;
      this.packageFullyQualifiedName = JavaPackage.DEFAULT_PACKAGE_NAME;
      realKey = new StringBuilder().append(JavaPackage.DEFAULT_PACKAGE_NAME).append(".").append(realKey).toString();
    }
    setDeprecatedKey(realKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JavaPackage getParent() {
    if (parent == null) {
      parent = new JavaPackage(packageFullyQualifiedName);
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
    return StringUtils.isNotBlank(filename) ? filename : (className + JAVA_SUFFIX);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLongName() {
    return fullyQualifiedName;
  }

  /**
   * @return SCOPE_ENTITY
   */
  @Override
  public String getScope() {
    return Scopes.FILE;
  }

  /**
   * @return QUALIFIER_UNIT_TEST_CLASS or QUALIFIER_FILE depending whether it is a unit test class
   */
  @Override
  public String getQualifier() {
    return unitTest ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
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
    WildcardPattern matcher = WildcardPattern.create(antPattern, Directory.SEPARATOR);
    return matcher.match(getKey());
  }

  /**
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public static JavaFile fromIOFile(File file, Project module, boolean unitTest) {
    if (file == null || !StringUtils.endsWithIgnoreCase(file.getName(), JAVA_SUFFIX)) {
      return null;
    }
    PathResolver.RelativePath relativePath = new PathResolver().relativePath(
      unitTest ? module.getFileSystem().getTestDirs() : module.getFileSystem().getSourceDirs(),
      file);
    if (relativePath != null) {
      JavaFile sonarFile = fromRelativePath(relativePath.path(), unitTest);
      sonarFile.setPath(new PathResolver().relativePath(module.getFileSystem().getBasedir(), file));
      return sonarFile;
    }
    return null;
  }

  /**
   * For internal use only.
   */
  public static JavaFile create(String relativePathFromBasedir) {
    JavaFile javaFile = new JavaFile();
    String normalizedPath = normalize(relativePathFromBasedir);
    javaFile.setKey(normalizedPath);
    javaFile.setPath(normalizedPath);
    String directoryKey = StringUtils.substringBeforeLast(normalizedPath, Directory.SEPARATOR);
    javaFile.parent = new Directory();
    javaFile.parent.setKey(directoryKey);
    return javaFile;
  }

  public static JavaFile create(String relativePathFromBasedir, String relativePathFromSourceDir, boolean unitTest) {
    JavaFile javaFile = JavaFile.create(relativePathFromBasedir);
    if (relativePathFromSourceDir.contains(Directory.SEPARATOR)) {
      javaFile.packageFullyQualifiedName = StringUtils.substringBeforeLast(relativePathFromSourceDir, Directory.SEPARATOR);
      javaFile.packageFullyQualifiedName = StringUtils.replace(javaFile.packageFullyQualifiedName, Directory.SEPARATOR, ".");
      javaFile.filename = StringUtils.substringAfterLast(relativePathFromSourceDir, Directory.SEPARATOR);
      if (javaFile.filename.endsWith(JAVA_SUFFIX)) {
        javaFile.className = StringUtils.removeEndIgnoreCase(javaFile.filename, JAVA_SUFFIX);
      } else if (javaFile.filename.endsWith(JAV_SUFFIX)) {
        javaFile.className = StringUtils.removeEndIgnoreCase(javaFile.filename, JAV_SUFFIX);
      }
      javaFile.fullyQualifiedName = javaFile.packageFullyQualifiedName + "." + javaFile.className;
      javaFile.setDeprecatedKey(javaFile.fullyQualifiedName);
      javaFile.parent.setDeprecatedKey(Directory.parseKey(StringUtils.substringBeforeLast(relativePathFromSourceDir, Directory.SEPARATOR)));
    } else {
      javaFile.packageFullyQualifiedName = JavaPackage.DEFAULT_PACKAGE_NAME;
      javaFile.className = StringUtils.removeEndIgnoreCase(relativePathFromSourceDir, JAVA_SUFFIX);
      javaFile.fullyQualifiedName = javaFile.className;
      javaFile.setDeprecatedKey(JavaPackage.DEFAULT_PACKAGE_NAME + "." + javaFile.className);
      javaFile.parent.setDeprecatedKey(Directory.ROOT);
    }
    javaFile.unitTest = unitTest;
    return javaFile;
  }

  /**
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
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
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
  public static JavaFile fromIOFile(File file, List<File> sourceDirs, boolean unitTest) {
    if (file == null || !StringUtils.endsWithIgnoreCase(file.getName(), JAVA_SUFFIX)) {
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
   * @deprecated since 4.2 use {@link #create(String, String, boolean)}
   */
  @Deprecated
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
      .append("deprecatedKey", getDeprecatedKey())
      .append("path", getPath())
      .append("filename", className)
      .toString();
  }

}
